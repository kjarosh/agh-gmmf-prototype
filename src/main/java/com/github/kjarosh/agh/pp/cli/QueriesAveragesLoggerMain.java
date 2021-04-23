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

    private static List<Duration> indexedMaxMembers = new ArrayList<>();
    private static List<Duration> naiveMaxMembers = new ArrayList<>();
    private static List<Duration> indexedAvgMembers = new ArrayList<>();
    private static List<Duration> naiveAvgMembers = new ArrayList<>();

    private static List<Duration> indexedMaxReachesExist = new ArrayList<>();
    private static List<Duration> naiveMaxReachesExist = new ArrayList<>();
    private static List<Duration> indexedAvgReachesExist = new ArrayList<>();
    private static List<Duration> naiveAvgReachesExist = new ArrayList<>();

    private static List<Duration> indexedMaxReachesNonexist = new ArrayList<>();
    private static List<Duration> naiveMaxReachesNonexist = new ArrayList<>();
    private static List<Duration> indexedAvgReachesNonexist = new ArrayList<>();
    private static List<Duration> naiveAvgReachesNonexist = new ArrayList<>();

    private static List<Duration> indexedMaxEp = new ArrayList<>();
    private static List<Duration> naiveMaxEp = new ArrayList<>();
    private static List<Duration> indexedAvgEp = new ArrayList<>();
    private static List<Duration> naiveAvgEp = new ArrayList<>();

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

                switch (next.getType()) {
                    case MEMBER:
                        if (next.getNaive()) {
                            naiveMaxMembers.add(next.getMaxDuration());
                            naiveAvgMembers.add(next.getAvgDuration());
                        } else {
                            indexedMaxMembers.add(next.getMaxDuration());
                            indexedAvgMembers.add(next.getAvgDuration());
                        }
                        break;
                    case REACHES:
                        if (next.getLabel() != null && next.getLabel().equals("exist")) {
                            if (next.getNaive()) {
                                naiveMaxReachesExist.add(next.getMaxDuration());
                                naiveAvgReachesExist.add(next.getAvgDuration());
                            } else {
                                indexedMaxReachesExist.add(next.getMaxDuration());
                                indexedAvgReachesExist.add(next.getAvgDuration());
                            }
                        } else {
                            if (next.getNaive()) {
                                naiveMaxReachesNonexist.add(next.getMaxDuration());
                                naiveAvgReachesNonexist.add(next.getAvgDuration());
                            } else {
                                indexedMaxReachesNonexist.add(next.getMaxDuration());
                                indexedAvgReachesNonexist.add(next.getAvgDuration());
                            }
                        }
                        break;
                    case EFFECTIVE_PERMISSIONS:
                        if (next.getNaive()) {
                            naiveMaxEp.add(next.getMaxDuration());
                            naiveAvgEp.add(next.getAvgDuration());
                        } else {
                            indexedMaxEp.add(next.getMaxDuration());
                            indexedAvgEp.add(next.getAvgDuration());
                        }
                        break;
                }
            }
        } catch (NullPointerException error) {
            System.out.println("Type - MEMBERS:");
            System.out.println("\tNaive:");
            System.out.println("\t\tmax - " + (naiveMaxMembers.stream().reduce(Duration::plus).orElseThrow()).dividedBy(naiveMaxMembers.size()));
            System.out.println("\t\tavg - " + (naiveAvgMembers.stream().reduce(Duration::plus).orElseThrow()).dividedBy(naiveAvgMembers.size()));
            System.out.println("\tIndexed:");
            System.out.println("\t\tmax - " + (indexedMaxMembers.stream().reduce(Duration::plus).orElseThrow()).dividedBy(indexedMaxMembers.size()));
            System.out.println("\t\tavg - " + (indexedAvgMembers.stream().reduce(Duration::plus).orElseThrow()).dividedBy(indexedAvgMembers.size()));

            System.out.println("Type - REACHES (existing):");
            System.out.println("\tNaive:");
            System.out.println("\t\tmax - " + (naiveMaxReachesExist.stream().reduce(Duration::plus).orElseThrow()).dividedBy(naiveMaxReachesExist.size()));
            System.out.println("\t\tavg - " + (naiveAvgReachesExist.stream().reduce(Duration::plus).orElseThrow()).dividedBy(naiveAvgReachesExist.size()));
            System.out.println("\tIndexed:");
            System.out.println("\t\tmax - " + (indexedMaxReachesExist.stream().reduce(Duration::plus).orElseThrow()).dividedBy(indexedMaxReachesExist.size()));
            System.out.println("\t\tavg - " + (indexedAvgReachesExist.stream().reduce(Duration::plus).orElseThrow()).dividedBy(indexedAvgReachesExist.size()));

            System.out.println("Type - REACHES (nonexisting):");
            System.out.println("\tNaive:");
            System.out.println("\t\tmax - " + (naiveMaxReachesNonexist.stream().reduce(Duration::plus).orElseThrow()).dividedBy(naiveMaxReachesNonexist.size()));
            System.out.println("\t\tavg - " + (naiveAvgReachesNonexist.stream().reduce(Duration::plus).orElseThrow()).dividedBy(naiveAvgReachesNonexist.size()));
            System.out.println("\tIndexed:");
            System.out.println("\t\tmax - " + (indexedMaxReachesNonexist.stream().reduce(Duration::plus).orElseThrow()).dividedBy(indexedMaxReachesNonexist.size()));
            System.out.println("\t\tavg - " + (indexedAvgReachesNonexist.stream().reduce(Duration::plus).orElseThrow()).dividedBy(indexedAvgReachesNonexist.size()));

            System.out.println("Type - EFFECTIVE_PERMISSIONS:");
            System.out.println("\tNaive:");
            System.out.println("\t\tmax - " + (naiveMaxEp.stream().reduce(Duration::plus).orElseThrow()).dividedBy(naiveMaxEp.size()));
            System.out.println("\t\tavg - " + (naiveAvgEp.stream().reduce(Duration::plus).orElseThrow()).dividedBy(naiveAvgEp.size()));
            System.out.println("\tIndexed:");
            System.out.println("\t\tmax - " + (indexedMaxEp.stream().reduce(Duration::plus).orElseThrow()).dividedBy(indexedMaxEp.size()));
            System.out.println("\t\tavg - " + (indexedAvgEp.stream().reduce(Duration::plus).orElseThrow()).dividedBy(indexedAvgEp.size()));
        }
    }
}
