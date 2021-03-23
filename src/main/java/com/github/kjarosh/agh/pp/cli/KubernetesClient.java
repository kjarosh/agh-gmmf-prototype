package com.github.kjarosh.agh.pp.cli;

import com.github.kjarosh.agh.pp.cli.utils.LogbackUtils;
import com.github.kjarosh.agh.pp.k8s.K8sConstantLoadClient;
import com.github.kjarosh.agh.pp.k8s.K8sZone;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.JSON;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import lombok.SneakyThrows;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Kamil Jarosz
 */
public class KubernetesClient {
    private static String namespace;
    private static int portOffset;

    static {
        LogbackUtils.loadLogbackCli();
    }

    public static void main(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("c", "config", true, "path to kubectl config");
        options.addOption("n", "namespace", true, "k8s namespace");
        options.addOption("p", "port-offset", true, "port offset for node ports, default 30080");
        options.addOption("z", "zones", true, "number of zones");
        options.addOption("g", "graph", true, "path to the graph to use");
        options.addOption(null, "constant-load-opts", true, "run constant load with these options");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        setupConfig(cmd.getOptionValue("c"));

        namespace = cmd.getOptionValue("n", "default");
        portOffset = Integer.parseInt(cmd.getOptionValue("p", "30080"));

        try {
            if (cmd.hasOption("z")) {
                int zones = Integer.parseInt(cmd.getOptionValue("z"));
                setupZones(zones);
            }

            if (cmd.hasOption("g") && cmd.hasOption("constant-load-opts")) {
                String constantLoadOpts = cmd.getOptionValue("constant-load-opts");
                Path graphPath = Paths.get(cmd.getOptionValue("g"));
                setupConstantLoad(constantLoadOpts, graphPath);
            }
        } catch (ApiException e) {
            processApiException(e);
        }
    }

    @SneakyThrows
    private static void setupConfig(String configPath) {
        Path path;
        if (configPath != null) {
            path = Paths.get(configPath);
        } else {
            path = Paths.get(System.getProperty("user.home"))
                    .resolve(".kube/config");
        }

        try (
                InputStream is = Files.newInputStream(path);
                Reader reader = new InputStreamReader(is)) {
            ApiClient client = Config.fromConfig(KubeConfig.loadKubeConfig(reader));
            Configuration.setDefaultApiClient(client);
        }
    }

    private static void setupZones(int zones) throws ApiException {
        for (int zone = 0; zone < zones; ++zone) {
            new K8sZone(namespace, "zone" + zone, portOffset + zone).apply();
        }
    }

    private static void setupConstantLoad(String constantLoadOpts, Path graphPath) throws ApiException {
        try (InputStream is = Files.newInputStream(graphPath)) {
            byte[] contents = ByteStreams.toByteArray(is);
            new K8sConstantLoadClient(namespace, contents, constantLoadOpts)
                    .apply();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void processApiException(ApiException e) {
        Gson gson = JSON.createGson().setPrettyPrinting().create();
        System.err.println("ApiException: " + e.getCode() + " " + e.getMessage());
        System.err.println(gson.toJson(gson.fromJson(e.getResponseBody(), Object.class)));
        e.printStackTrace();
    }
}
