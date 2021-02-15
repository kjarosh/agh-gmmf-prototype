package com.github.kjarosh.agh.pp.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.kjarosh.agh.pp.cli.utils.LogbackUtils;
import com.github.kjarosh.agh.pp.config.Config;
import com.github.kjarosh.agh.pp.config.ConfigLoader;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLWarning;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Kamil Jarosz
 */
@Slf4j
public class GatherResultsMain {
    private static final ExecutorService executor = Executors.newFixedThreadPool(6);
    private static final String[] zones = new String[]{"krakow-lo", "lisbon-lo", "zone-lo"};
    private static final String keysPath = System.getProperty("app.keys_path");
    private static final String graphPath = System.getProperty("app.graph");
    private static final Config config = ConfigLoader.getConfig();
    private static final Map<String, SSHClient> clients = new HashMap<>();

    private static final GatherConfig[] configs = new GatherConfig[]{
            buildGatherConfig()
                    .operationsPerSecond(100)
                    .build(),
            buildGatherConfig()
                    .operationsPerSecond(200)
                    .build(),
            buildGatherConfig()
                    .operationsPerSecond(300)
                    .build(),
            buildGatherConfig()
                    .operationsPerSecond(400)
                    .build(),
            buildGatherConfig()
                    .operationsPerSecond(500)
                    .build()
    };

    static {
        LogbackUtils.loadLogbackCli();
    }

    private static GatherConfig.GatherConfigBuilder buildGatherConfig() {
        return GatherConfig.builder()
                .loadDuration(Duration.ofMinutes(10))
                .analysisStartPercent(30)
                .analysisEndPercent(90)
                .requestsPerSecond(10)
                .clientThreads(9);
    }

    public static void main(String[] args) throws IOException, MavenInvocationException {
        try {
            Path resultsPath = Paths.get("results").resolve("results-" + Instant.now());
            Files.createDirectory(resultsPath);
            initializeSshClients();
            int count = 0;
            for (GatherConfig gc : configs) {
                ++count;
                log.info("==================== Config {}/{}", count, configs.length);
                try {
                    gather(resultsPath, gc);
                } catch (Exception e) {
                    log.error("Error gathering", e);
                }
            }
        } finally {
            executor.shutdownNow();

            for (SSHClient c : clients.values()) {
                c.disconnect();
            }
        }
    }

    private static void gather(Path resultsPath, GatherConfig gc) throws IOException {
        Path resultPath = resultsPath.resolve("rps=" + gc.getOperationsPerSecond());
        Files.createDirectories(resultPath);
        runSaved();
        saveObject(graphPath, resultPath.resolve("graph_path"));
        saveObject(gc, resultPath.resolve("settings.json"));
        saveRevision(resultPath.resolve("git_revision"));
        Files.copy(Paths.get(graphPath), resultPath.resolve("graph.json"));
        Instant start = Instant.now();
        runConstantLoad(gc, resultPath.resolve("output.log"));
        Instant end = Instant.now();
        gatherLogs(resultPath.resolve("logs"));
        gatherArtifacts(resultPath.resolve("artifacts"));

        Duration loadDuration = Duration.between(start, end);
        AnalysisConfig ac = AnalysisConfig.builder()
                .loadStart(start)
                .loadEnd(end)
                .analysisStart(start.plus(loadDuration.multipliedBy(gc.getAnalysisStartPercent()).dividedBy(100)))
                .analysisEnd(start.plus(loadDuration.multipliedBy(gc.getAnalysisEndPercent()).dividedBy(100)))
                .build();
        saveObject(ac, resultPath.resolve("analysis_config.json"));
        String reportSql = generateReportSql(ac);
        saveString(reportSql, resultPath.resolve("report.sql"));

        loadDatabase(resultPath.resolve("artifacts"), resultPath.resolve("load_database.log"));
        runReport(reportSql, resultPath.resolve("postgres_report.log"));
    }

    @SneakyThrows
    private static void runReport(String reportSql, Path output) {
        log.info("Running report to {}", output);
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost/postgres", "postgres", "admin")) {
            PreparedStatement ps = conn.prepareStatement(reportSql);
            ps.execute();
            SQLWarning w = ps.getWarnings();
            StringBuilder log = new StringBuilder();
            while (w != null) {
                log.append(w.getMessage()).append("\n");
                w = w.getNextWarning();
            }
            saveString(log.toString(), output);
        }
    }

    @SneakyThrows
    private static void loadDatabase(Path artifacts, Path logPath) {
        log.info("Loading database from {}", artifacts);
        try (OutputStream os = Files.newOutputStream(logPath)) {
            InvocationOutputHandler oh = line -> os.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            mavenExec(PostgresImportMain.class, artifacts.toString(), new Properties(), oh);
        }
    }

    private static String generateReportSql(AnalysisConfig ac) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
        String start = format.format(Date.from(ac.getAnalysisStart()));
        String end = format.format(Date.from(ac.getAnalysisEnd()));
        return "" +
                "do $$ begin\n" +
                "  perform report(timestamp '" + start + "', timestamp '" + end + "');\n" +
                "end $$";
    }

    @SneakyThrows
    private static void gatherArtifacts(Path artifacts) {
        Files.createDirectories(artifacts);
        log.info("Gathering artifacts to {}", artifacts);
        List<Future<?>> futures = new ArrayList<>();
        for (String zone : zones) {
            futures.add(executor.submit(() -> {
                try (SFTPClient sftp = clients.get(zone).newSFTPClient()) {
                    Path dest = artifacts.resolve(zone);
                    sftp.get("/home/ubuntu/kjarosz/artifacts/", dest.toString());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }
    }

    @SneakyThrows
    private static void gatherLogs(Path logs) {
        Files.createDirectories(logs);
        log.info("Gathering logs to {}", logs);
        List<Future<?>> futures = new ArrayList<>();
        for (String zone : zones) {
            futures.add(executor.submit(() -> {
                try (Session session = clients.get(zone).startSession()) {
                    Session.Command cmd = session.exec("cd /home/ubuntu/kjarosz && ./logs.sh");
                    consumeStreamToFile(cmd.getInputStream(), "", logs.resolve(zone + ".log"));
                    consumeStreamToFile(cmd.getErrorStream(), "", logs.resolve(zone + ".err.log"));
                    cmd.join();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }
    }

    @SneakyThrows
    private static void saveRevision(Path path) {
        log.info("Saving revision to {}", path);
        Process process = new ProcessBuilder()
                .command("git", "rev-parse", "HEAD")
                .redirectOutput(path.toFile())
                .start();
        if (process.waitFor() != 0) {
            throw new RuntimeException();
        }
    }

    @SneakyThrows
    private static void saveString(String str, Path output) {
        log.info("Saving {}", output);
        try (OutputStream os = Files.newOutputStream(output)) {
            os.write(str.getBytes(StandardCharsets.UTF_8));
        }
    }

    @SneakyThrows
    private static void saveObject(Object obj, Path output) {
        log.info("Saving {}", output);
        new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .writerWithDefaultPrettyPrinter()
                .writeValue(output.toFile(), obj);
    }

    private static void initializeSshClients() {
        log.info("Initializing SSH connections");
        for (String zone : zones) {
            clients.put(zone, connectToZone(zone));
        }
        log.info("Connections initialized");
    }

    @SneakyThrows
    private static void runConstantLoad(GatherConfig gc, Path logPath) {
        log.info("Running constant load (saving to {})", logPath);
        Properties props = new Properties();
        try (OutputStream os = Files.newOutputStream(logPath)) {
            props.setProperty("app.config_path", "remote/sync/config.json");
            InvocationOutputHandler oh = line -> os.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            mavenExec(ConstantLoadClientMain.class, "" +
                    " -b " + (gc.getOperationsPerSecond() / gc.getRequestsPerSecond()) +
                    " -n " + gc.getOperationsPerSecond() +
                    " -g " + graphPath +
                    " -t " + gc.getClientThreads() +
                    " -d " + gc.getLoadDuration().toSeconds(), props, oh);
        }
    }

    @SneakyThrows
    private static void runSaved() {
        log.info("Starting containers from the saved state");
        executeOnAllZones("cd /home/ubuntu/kjarosz && ./run.sh kjarosh/ms-graph-simulator:saved false");
        int secsWait = 10;
        log.info("Containers are up and running, waiting " + secsWait + " seconds for application startup");
        Thread.sleep(secsWait * 1000);
        log.info("Ready");

    }

    @SneakyThrows
    private static void executeOnAllZones(String command) {
        List<Future<?>> futures = new ArrayList<>();
        for (String zone : zones) {
            futures.add(executor.submit(() -> executeOnZone(zone, command)));
        }

        for (Future<?> future : futures) {
            future.get();
        }
    }

    @SneakyThrows
    private static void executeOnZone(String zone, String command) {
        SSHClient ssh = clients.get(zone);
        try (Session session = ssh.startSession()) {
            Session.Command cmd = session.exec(command);
            Future<?> err = executor.submit(() -> {
                consumeStreamToStdout(cmd.getInputStream(), "[ssh/" + zone + "/err] ");
            });
            consumeStreamToStdout(cmd.getInputStream(), "[ssh/" + zone + "] ");
            err.get();
            cmd.join();
            if (cmd.getExitStatus() != 0) {
                throw new RuntimeException("Command '" + command + "' failed on " + zone + " (" + cmd.getExitStatus() + ")");
            }
        }
    }

    @SneakyThrows
    private static void consumeStreamToFile(InputStream is, String prefix, Path file) {
        try (OutputStream os = Files.newOutputStream(file)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                os.write((prefix + line + "\n").getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    @SneakyThrows
    private static void consumeStreamToStdout(InputStream is, String prefix) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            log.info(prefix + line);
        }
    }

    @SneakyThrows
    private static SSHClient connectToZone(String zone) {
        SSHClient ssh = new SSHClient();
        ssh.loadKnownHosts();
        ssh.addHostKeyVerifier((hostname, port, key) -> true);
        ssh.connect(config.getZones().get(zone).getAddress().split(":", 2)[0]);
        ssh.authPublickey("ubuntu", ssh.loadKeys(keysPath));
        return ssh;
    }

    @SneakyThrows
    private static void mavenExec(Class<?> mainClass, String args, Properties userProps, InvocationOutputHandler oh) {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File("pom.xml"));
        request.setBatchMode(true);
        request.setGoals(Collections.singletonList("exec:java"));
        Properties props = new Properties();
        props.setProperty("exec.mainClass", mainClass.getName());
        props.setProperty("exec.args", args);
        props.setProperty("exec.cleanupDaemonThreads", "false");
        if (userProps != null) {
            userProps.forEach(props::put);
        }
        request.setProperties(props);

        Invoker invoker = new DefaultInvoker();
        if (oh != null) {
            invoker.setOutputHandler(s -> {
                System.out.println("[maven/stdout] " + s);
                oh.consumeLine("[stdout] " + s);
            });
            invoker.setErrorHandler(s -> {
                System.out.println("[maven/stderr] " + s);
                oh.consumeLine("[stderr] " + s);
            });
        }
        invoker.execute(request);
    }

    @Getter
    @Builder
    static class GatherConfig {
        private final int clientThreads;
        private final int requestsPerSecond;
        private final int operationsPerSecond;
        private final Duration loadDuration;
        private final int analysisStartPercent;
        private final int analysisEndPercent;
    }

    @Getter
    @Builder
    static class AnalysisConfig {
        private final Instant loadStart;
        private final Instant analysisStart;
        private final Instant loadEnd;
        private final Instant analysisEnd;
    }
}
