package com.github.kjarosh.agh.pp.graph.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.kjarosh.agh.pp.graph.util.Query;
import com.github.kjarosh.agh.pp.graph.util.QueryClientResults;
import com.github.kjarosh.agh.pp.graph.util.QueryType;
import lombok.SneakyThrows;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class QueryClientResultsWriter {
    private final PrintStream file;
    private static final ObjectWriter writer = new ObjectMapper().writerFor(QueryClientResults.class);

    public QueryClientResultsWriter(String filepath) throws IOException {
        file = _setupOutputStream(filepath);
    }

    private static PrintStream _setupOutputStream(String filepath) throws IOException {
        var path = Path.of(filepath);

        if(Files.isDirectory(path)) {
            path = Path.of(filepath, "queries_results_"+ DateTime.now()+".json");
        }
        //Files.deleteIfExists(path);

        return new PrintStream(Files.newOutputStream(path, CREATE, APPEND));
    }

    public void save() {
        file.flush();
        file.close();
    }

    @SneakyThrows
    public void put(QueryType type, boolean naive, LinkedHashMap max, LinkedHashMap avg, String label) {
        QueryClientResults results = QueryClientResults.builder()
                .type(type)
                .naive(naive)
                .max(max)
                .avg(avg)
                .label(label)
                .build();
        file.println(writer.writeValueAsString(results));
    }
}
