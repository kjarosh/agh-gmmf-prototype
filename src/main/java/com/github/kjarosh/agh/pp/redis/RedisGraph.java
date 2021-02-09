package com.github.kjarosh.agh.pp.redis;

import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.VertexId;

import java.io.OutputStream;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    protected String keyEdge(EdgeId id) {
        return keyEdge(id.getFrom().toString(), id.getTo().toString());
    }

    @Override
    public void serialize(OutputStream os) {
        throw new UnsupportedOperationException("Redis graph cannot be serialized");
    }

    protected abstract Stream<EdgeId> getEdgeIdsByKeyPattern(String keyPattern);

    @Override
    public Set<VertexId> getDestinationsBySource(VertexId source) {
        return getEdgeIdsByKeyPattern(keyEdge(source.toString(), "*"))
                .map(EdgeId::getTo)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<VertexId> getSourcesByDestination(VertexId destination) {
        return getEdgeIdsByKeyPattern(keyEdge("*", destination.toString()))
                .map(EdgeId::getFrom)
                .collect(Collectors.toSet());
    }

    protected abstract Set<Edge> getEdgesByKeyPattern(String keyPattern);

    @Override
    public Set<Edge> getEdgesBySource(VertexId source) {
        return getEdgesByKeyPattern(keyEdge(source.toString(), "*"));
    }

    @Override
    public Set<Edge> getEdgesByDestination(VertexId destination) {
        return getEdgesByKeyPattern(keyEdge("*", destination.toString()));
    }
}
