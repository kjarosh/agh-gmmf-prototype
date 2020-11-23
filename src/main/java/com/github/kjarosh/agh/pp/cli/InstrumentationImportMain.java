package com.github.kjarosh.agh.pp.cli;

import com.github.kjarosh.agh.pp.instrumentation.Notification;
import com.github.kjarosh.agh.pp.instrumentation.jpa.DbNotification;
import com.google.common.io.CharStreams;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

/**
 * @author Kamil Jarosz
 */
public class InstrumentationImportMain {
    private static final Logger logger = LoggerFactory.getLogger(InstrumentationImportMain.class);

    private static final String PERSISTENCE_UNIT_NAME = "postgres-import";

    public static void main(String[] args) throws IOException {
        if (args.length < 1) System.exit(1);

        Path csv = Paths.get(args[0]);
        List<Path> csvFiles = new ArrayList<>();

        if (Files.isRegularFile(csv)) {
            logger.info("{} is a regular file, importing it", csv);
            csvFiles.add(csv);
        } else if (Files.isDirectory(csv)) {
            logger.info("{} is a directory, searching for CSV files", csv);
            Files.walk(csv)
                    .filter(file -> file.toString().endsWith(".csv"))
                    .forEach(csvFiles::add);
        }

        logger.info("Importing the following files: {}", csvFiles);

        Persistence.generateSchema(PERSISTENCE_UNIT_NAME, new HashMap<>());
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        EntityManager em = factory.createEntityManager();
        try {
            em.getTransaction().begin();
            createViews(em);
            em.getTransaction().commit();

            em.getTransaction().begin();
            for (Path csvFile : csvFiles) {
                logger.info("Importing {}", csvFile);
                importCsv(csvFile, em);
            }
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    private static void createViews(EntityManager em) {
        try (InputStream is = InstrumentationImportMain.class
                .getClassLoader()
                .getResourceAsStream("postgres/views.sql")) {
            Objects.requireNonNull(is);
            String sql = CharStreams.toString(new InputStreamReader(is));
            em.createNativeQuery(sql).executeUpdate();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void importCsv(Path csv, EntityManager em) {
        try (BufferedReader reader = Files.newBufferedReader(csv)) {
            reader.lines()
                    .map(InstrumentationImportMain::parseNotification)
                    .forEach(em::persist);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static DbNotification parseNotification(String line) {
        List<String> values = getCsvFields(line);
        List<String> fields = new ArrayList<>();
        Notification.serializers.forEach((field, v) -> fields.add(field));

        DbNotification notification = new DbNotification();

        int i = 0;
        for (String field : fields) {
            String value = StringEscapeUtils.unescapeCsv(values.get(i));
            setNotificationField(notification, field, value);
            ++i;
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
            case "eventType":
                notification.setEventType(value);
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
