package com.github.kjarosh.agh.pp.graph.query;

import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.util.Query;
import com.github.kjarosh.agh.pp.graph.util.QueryType;
import com.github.kjarosh.agh.pp.util.JsonLinesWriter;

import java.io.OutputStream;

public class QueriesWriter {
    private final JsonLinesWriter writer;

    public QueriesWriter(OutputStream os) {
        this.writer = new JsonLinesWriter(os);
    }

    private void put(Query query) {
        synchronized (this) {
            writer.writeValue(query);
        }
    }

    public void member(VertexId from) {
        put(Query.builder()
                .type(QueryType.MEMBER)
                .from(from)
                .to(null)
                .existing(true)
                .build());
    }

    public void reaches(VertexId from, VertexId to, boolean existing) {
        put(Query.builder()
                .type(QueryType.REACHES)
                .from(from)
                .to(to)
                .existing(existing)
                .build());
    }

    public void effectivePermissions(VertexId from, VertexId to, boolean existing) {
        put(Query.builder()
                .type(QueryType.EFFECTIVE_PERMISSIONS)
                .from(from)
                .to(to)
                .existing(existing)
                .build());
    }
}
