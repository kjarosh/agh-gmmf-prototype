package com.github.kjarosh.agh.pp.graph.model;

import com.github.kjarosh.agh.pp.Config;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.kjarosh.agh.pp.Config.ZONE_ID;

/**
 * @author Kamil Jarosz
 */
public class Graph {
    private final Map<VertexId, ZoneId> vertexOwners = new HashMap<>();
    private final Map<VertexId, Vertex> vertices = new HashMap<>();
    private final Set<Edge> edges = new HashSet<>();
    private final Map<VertexId, Set<Edge>> edgesBySrc = new HashMap<>();
    private final Map<VertexId, Set<Edge>> edgesByDst = new HashMap<>();

    public Graph() {

    }

    @SneakyThrows
    public static Graph deserialize(InputStream serialized) {
        Json value = Config.MAPPER.readValue(serialized, Json.class);
        Graph graph = new Graph();
        value.vertices.forEach(graph::addVertex);
        value.edges.forEach(graph::addEdge);
        return graph;
    }

    public void addVertex(Vertex v) {
        vertexOwners.put(v.id(), v.zone());

        if (ZONE_ID == null || v.zone().equals(ZONE_ID)) {
            vertices.put(v.id(), v);
        }
    }

    public boolean hasVertex(VertexId id) {
        return vertices.containsKey(id);
    }

    public Vertex getVertex(VertexId id) {
        return vertices.get(id);
    }

    public ZoneId getVertexOwner(VertexId id) {
        return vertexOwners.get(id);
    }

    public void addEdge(Edge e) {
        if (ZONE_ID != null &&
                !hasVertex(e.src()) &&
                !hasVertex(e.dst())) {
            return;
        }

        edges.add(e);
        edgesBySrc.computeIfAbsent(e.src(), i -> new HashSet<>()).add(e);
        edgesByDst.computeIfAbsent(e.dst(), i -> new HashSet<>()).add(e);
    }

    public Set<Edge> getEdgesBySource(VertexId source) {
        return edgesBySrc.getOrDefault(source, Collections.emptySet());
    }

    public Set<Edge> getEdgesByDestination(VertexId destination) {
        return edgesByDst.getOrDefault(destination, Collections.emptySet());
    }

    public int edgeCount() {
        return edges.size();
    }

    public Collection<Vertex> allVertices() {
        return vertices.values();
    }

    public Collection<Edge> allEdges() {
        return edges;
    }

    @SneakyThrows
    public void serialize(OutputStream os) {
        Json json = new Json();
        json.setEdges(new ArrayList<>(this.edges));
        json.setVertices(new ArrayList<>(this.vertices.values()));
        Config.MAPPER.writeValue(os, json);
    }

    @Override
    public String toString() {
        return "Graph(" + edges.size() + " edges, " + vertices.size() + " vertices)";
    }

    @Getter
    @Setter
    private static class Json {
        private List<Vertex> vertices;
        private List<Edge> edges;
    }
}
