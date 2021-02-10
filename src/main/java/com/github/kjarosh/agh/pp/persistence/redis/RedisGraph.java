package com.github.kjarosh.agh.pp.persistence.redis;

import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;

import java.io.OutputStream;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kamil Jarosz
 */
public abstract class RedisGraph implements Graph {
    private final Pattern keyEdgePattern;
    private final String prefix;

    public RedisGraph(String prefix) {
        this.prefix = prefix;
        this.keyEdgePattern = Pattern.compile(keyEdge("([^/]+)", "([^/]+)"));
    }

    protected String keyZones() {
        return prefix + "/zones";
    }

    protected String keyVertices() {
        return prefix + "/vertices";
    }

    protected String keyVertex(VertexId id) {
        return keyVertices() + "/" + id.toString();
    }

    protected String keyEdges() {
        return prefix + "/edges";
    }

    protected String keyEdge(String src, String dst) {
        return keyEdges() + "/" + src + "/" + dst;
    }

    protected String keyEdge(EdgeId id) {
        return keyEdge(id.getFrom().toString(), id.getTo().toString());
    }

    protected String keyEdgeBySrc(VertexId src) {
        return prefix + "/edges/by-src/" + src.toString();
    }

    protected String keyEdgeByDst(VertexId dst) {
        return prefix + "/edges/by-dst/" + dst.toString();
    }

    protected String[] parseEdgeFromKey(String key) {
        Matcher matcher = keyEdgePattern.matcher(key);
        if (!matcher.matches()) {
            throw new RuntimeException("Key doesn't match: " + key);
        }

        return new String[]{
                matcher.group(1),
                matcher.group(2)
        };
    }

    @Override
    public void serialize(OutputStream os) {
        throw new UnsupportedOperationException("Redis graph cannot be serialized");
    }

    @Override
    public Collection<Vertex> allVertices() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Edge> allEdges() {
        throw new UnsupportedOperationException();
    }

}
