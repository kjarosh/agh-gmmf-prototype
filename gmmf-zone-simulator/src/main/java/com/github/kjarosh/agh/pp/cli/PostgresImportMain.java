package com.github.kjarosh.agh.pp.cli;

import com.github.kjarosh.agh.pp.cli.utils.LogbackUtils;
import com.github.kjarosh.agh.pp.instrumentation.Notification;
import com.github.kjarosh.agh.pp.instrumentation.jpa.DbNotification;
import com.google.common.io.CharStreams;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.hibernate.Session;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class PostgresImportMain {
    private static final String PERSISTENCE_UNIT_NAME = "postgres-import";
    private static final int BATCH_SIZE = 100000;

    static {
        LogbackUtils.loadLogbackCli();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) System.exit(1);

        Path csv = Paths.get(args[0]);
        List<Path> csvFiles = new ArrayList<>();

        if (Files.isRegularFile(csv)) {
            log.info("{} is a regular file, importing it", csv);
            csvFiles.add(csv);
        } else if (Files.isDirectory(csv)) {
            log.info("{} is a directory, searching for CSV files", csv);
            Files.walk(csv)
                    .filter(file -> file.toString().endsWith(".csv"))
                    .forEach(csvFiles::add);
        }

        log.info("Importing the following files: {}", csvFiles);

        Persistence.generateSchema(PERSISTENCE_UNIT_NAME, new HashMap<>());
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            log.info("Creating objects");
            createObjects(em);
            em.getTransaction().commit();

            em.getTransaction().begin();
            em.createQuery("delete from DbNotification n").executeUpdate();
            importCsvsNew(csvFiles, em);
            log.info("Committing transaction");
            em.getTransaction().commit();

            em.getTransaction().begin();
            log.info("Refreshing views");
            refreshViews(em);
            em.getTransaction().commit();

        } finally {
            em.close();
        }
    }

    private static void refreshViews(EntityManager em) {
        executeSqlFile(em, "postgres/refresh_views.sql");
    }

    private static void createObjects(EntityManager em) {
        executeSqlFile(em, "postgres/views.sql");
        executeSqlFile(em, "postgres/functions.sql");
    }

    private static void executeSqlFile(EntityManager em, String path) {
        try (InputStream is = PostgresImportMain.class
                .getClassLoader()
                .getResourceAsStream(path)) {
            Objects.requireNonNull(is);
            String sql = CharStreams.toString(new InputStreamReader(is));
            em.createNativeQuery(sql).executeUpdate();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SneakyThrows
    private static void importCsvsNew(List<Path> csvFiles, EntityManager em) {
        PipedReader pipedReader = new PipedReader();
        PipedWriter pipedWriter = new PipedWriter();
        pipedWriter.connect(pipedReader);

        new Thread(() -> {
            for (Path csv : csvFiles) {
                log.info("Importing {}", csv);
                try (Stream<String> lines = Files.lines(csv)) {
                    lines.map(line -> "\"" + UUID.randomUUID() + "\"," + line)
                            .forEach(line -> {
                                try {
                                    pipedWriter.write(line.toCharArray());
                                    pipedWriter.write('\n');
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            try {
                pipedWriter.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).start();

        Session session = em.unwrap(Session.class);
        session.doWork(connection -> {
            try {
                long rowsInserted = new CopyManager((BaseConnection) connection)
                        .copyIn("COPY dbnotification (id,zone,time,thread,type,trace,eventId,eventType,vertex,sender,originalSender,forkChildren)" +
                                " FROM STDIN (FORMAT csv)", pipedReader);
                log.info("{} row(s) imported", rowsInserted);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static void importCsvsOld(List<Path> csvFiles, EntityManager em) {
        for (Path csvFile : csvFiles) {
            log.info("Importing {}", csvFile);
            importCsvOld(csvFile, em);
        }
    }

    private static void importCsvOld(Path csv, EntityManager em) {
        long total;
        try (BufferedReader reader = Files.newBufferedReader(csv)) {
            total = reader.lines().count();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        try (BufferedReader reader = Files.newBufferedReader(csv)) {
            AtomicInteger count = new AtomicInteger(0);
            reader.lines()
                    .map(PostgresImportMain::parseNotification)
                    .filter(Objects::nonNull)
                    .forEach(o -> {
                        int c = count.incrementAndGet();
                        if (c % BATCH_SIZE == 0) {
                            log.info("Imported {}/{} ({} %)", c, total, 100D * c / total);
                            em.getTransaction().commit();
                            em.clear();
                            System.gc();
                            em.getTransaction().begin();
                        }
                        em.persist(o);
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static DbNotification parseNotification(String line) {
        List<String> values = getCsvFields(line);
        List<String> fields = new ArrayList<>();
        Notification.serializers.forEach((field, v) -> fields.add(field));

        DbNotification notification = new DbNotification();
        if (values.size() < fields.size()) {
            return null;
        }

        try {
            int i = 0;
            for (String field : fields) {
                String value = StringEscapeUtils.unescapeCsv(values.get(i));
                setNotificationField(notification, field, value);
                ++i;
            }
        } catch (Exception e) {
            return null;
        }

        return notification;
    }

    private static List<String> getCsvFields(String line) {
        List<String> values = new ArrayList<>();
        try (Scanner rowScanner = new Scanner(line)) {
            rowScanner.useDelimiter(",");
            while (rowScanner.hasNext()) {
                values.add(rowScanner.next());
            }
        }
        return values;
    }

    private static void setNotificationField(DbNotification notification, String field, String value) {
        switch (field) {
            case "zone":
                notification.setZone(value);
                break;
            case "time":
                notification.setTime(Instant.parse(value));
                break;
            case "thread":
                notification.setThread(value);
                break;
            case "type":
                notification.setType(value);
                break;
            case "trace":
                notification.setTrace(value);
                break;
            case "eventId":
                notification.setEventId(value);
                break;
            case "eventType":
                notification.setEventType(value);
                break;
            case "vertex":
                notification.setVertex(value);
                break;
            case "sender":
                notification.setSender(value);
                break;
            case "originalSender":
                notification.setOriginalSender(value);
                break;
            case "forkChildren":
                notification.setForkChildren(Integer.parseInt(value));
                break;
            default:
                throw new RuntimeException("Unknown field: " + field);
        }
    }
}
