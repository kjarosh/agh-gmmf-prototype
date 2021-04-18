package com.github.kjarosh.agh.pp.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.kjarosh.agh.pp.graph.util.QueryClientResults;
import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class QueriesAveragesLoggerMain {
    private static BufferedReader fileReader;
    private static final ObjectReader objectReader = new ObjectMapper().readerFor(QueryClientResults.class);
    private static List<Duration> indexedMax = new ArrayList<>();
    private static List<Duration> naiveMax = new ArrayList<>();
    private static List<Duration> indexedAvg = new ArrayList<>();
    private static List<Duration> naiveAvg = new ArrayList<>();

    private static synchronized QueryClientResults next() throws IOException {
        var str = fileReader.readLine();
        return objectReader.readValue(str.trim());
    }

    public static void main(String[] args) throws ParseException, IOException {
        Options options = new Options();
        options.addRequiredOption("r", "results", true, "Path to file with results");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        var path = Path.of(cmd.getOptionValue("r"));
        if(!Files.exists(path) || Files.isDirectory(path)) {
            throw new IllegalArgumentException("path doesn't point to correct file");
        }

        fileReader = new BufferedReader(new InputStreamReader(Files.newInputStream(path)));

        try {
            while (true) {
                QueryClientResults next = next();

                if (next.getNaive()) {
                    naiveMax.add(Duration.ofNanos((Integer) ((LinkedHashMap) next.getMax()).get("nano")));
                    naiveAvg.add(Duration.ofNanos((Integer) ((LinkedHashMap) next.getMax()).get("nano")));
                } else {
                    indexedMax.add(Duration.ofNanos((Integer) ((LinkedHashMap) next.getMax()).get("nano")));
                    indexedAvg.add(Duration.ofNanos((Integer) ((LinkedHashMap) next.getMax()).get("nano")));
                }
            }
        } catch (NullPointerException error) {
            System.out.println("Naive:");
            System.out.println("max - " + (naiveMax.stream().reduce(Duration::plus).orElseThrow()).dividedBy(naiveMax.size()));
            System.out.println("avg - " + (naiveAvg.stream().reduce(Duration::plus).orElseThrow()).dividedBy(naiveMax.size()));
            System.out.println("Indexed:");
            System.out.println("max - " + (indexedMax.stream().reduce(Duration::plus).orElseThrow()).dividedBy(indexedMax.size()));
            System.out.println("avg - " + (indexedAvg.stream().reduce(Duration::plus).orElseThrow()).dividedBy(indexedMax.size()));
        }
    }
}
