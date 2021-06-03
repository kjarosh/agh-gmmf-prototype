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

    private void put(QueryType type, VertexId from, VertexId to, boolean existing) {
        writer.writeValue(new Query(type, from, to, existing));
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

    public void effectivePermissions(VertexId from, VertexId to, boolean existing) {
        put(
                QueryType.EFFECTIVE_PERMISSIONS,
                from,
                to,
                existing
        );
    }
}
