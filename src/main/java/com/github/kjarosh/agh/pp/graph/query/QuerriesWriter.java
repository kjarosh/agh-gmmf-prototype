package com.github.kjarosh.agh.pp.graph.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.util.Operation;
import com.github.kjarosh.agh.pp.graph.util.Query;
import com.github.kjarosh.agh.pp.graph.util.QueryType;
import lombok.SneakyThrows;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

public class QuerriesWriter {
    private final PrintStream file;
    private static final ObjectWriter writer = new ObjectMapper().writerFor(Query.class);

    public QuerriesWriter(String filepath) throws IOException {
        file = _setupOutputStream(filepath);
    }

    private static PrintStream _setupOutputStream(String filepath) throws IOException {
        var path = Path.of(filepath);

        if(Files.isDirectory(path)) {
            path = Path.of(filepath, "queries_"+ DateTime.now()+".json");
        }
        //Files.deleteIfExists(path);

        return new PrintStream(Files.newOutputStream(path, CREATE, APPEND));
    }

    public void save() {
        file.flush();
        file.close();
    }

    @SneakyThrows
    private void put(QueryType type, VertexId from, VertexId to, boolean existing) {
        put(new Query(type, from, to, existing));
    }

    @SneakyThrows
    private void put(Query query) {
        file.println(writer.writeValueAsString(query));
    }

    public void member(VertexId from) {
        put(
                Query.builder()
                    .type(QueryType.MEMBER)
                    .from(from)
                    .to(null)
                    .existing(true)
                    .build()
        );
    }

    public void reaches(VertexId from, VertexId to, boolean existing) {
        put(
                Query.builder()
                        .type(QueryType.REACHES)
                        .from(from)
                        .to(to)
                        .existing(existing)
                        .build()
        );
    }

    public void effectivePermisions(VertexId from, VertexId to, boolean existing) {
        put(
                Query.builder()
                        .type(QueryType.EFFECTIVE_PERMISSIONS)
                        .from(from)
                        .to(to)
                        .existing(existing)
                        .build()
        );
    }
}
