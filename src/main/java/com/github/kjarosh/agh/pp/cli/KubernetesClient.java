package com.github.kjarosh.agh.pp.cli;

import com.github.kjarosh.agh.pp.k8s.ZoneDeployment;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import lombok.SneakyThrows;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Kamil Jarosz
 */
public class KubernetesClient {
    private static String namespace;
    private static int zones;

    public static void main(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("c", "config", true, "path to kubectl config");
        options.addOption("n", "namespace", true, "k8s namespace");
        options.addRequiredOption("z", "zones", true, "number of zones");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        setupConfig(cmd.getOptionValue("c"));

        namespace = cmd.getOptionValue("n", "default");
        zones = Integer.parseInt(cmd.getOptionValue("z"));

        setupZones();
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

    @SneakyThrows
    private static void setupZones() {
        for (int zone = 0; zone < zones; ++zone) {
            new ZoneDeployment(namespace, "zone" + zone).apply();
        }
    }
}
