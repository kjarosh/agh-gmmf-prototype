package com.github.kjarosh.agh.pp.graph.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.util.Query;
import com.github.kjarosh.agh.pp.graph.util.QueryType;
import lombok.SneakyThrows;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class QuerriesWriter {
    private final Queue<Query> querries = new LinkedList<>();

    @SneakyThrows
    public void save(String filename) {
        File file = new File(filename);
        new ObjectMapper().writeValue(file, querries);
        querries.clear();
    }

    private void put(QueryType type, VertexId from, VertexId to, boolean existing) {
        querries.add(new Query(type, from, to, existing));
    }

    public void member(VertexId from) {
        put(
                QueryType.MEMBER,
                from,
                null,
                true
        );
    }

    public void reaches(VertexId from, VertexId to, boolean existing) {
        put(
                QueryType.REACHES,
                from,
                to,
                existing
        );
    }

    public void effectivePermisions(VertexId from, VertexId to, boolean existing) {
        put(
                QueryType.EFFECTIVE_PERMISSIONS,
                from,
                to,
                existing
        );
    }
}
