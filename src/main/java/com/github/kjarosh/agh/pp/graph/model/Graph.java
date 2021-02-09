package com.github.kjarosh.agh.pp.graph.model;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Set;

/**
 * The local graph representation. It contains only the local portion of the
 * distributed graph. Thread-safe.
 *
 * @author Kamil Jarosz
 */
public interface Graph {
    void addVertex(Vertex v);

    boolean hasVertex(VertexId id);

    boolean hasEdge(EdgeId edgeId);

    Vertex getVertex(VertexId id);

    void addEdge(Edge e);

    void removeEdge(Edge e);

    Edge getEdge(EdgeId edgeId);

    void setPermissions(EdgeId edgeId, Permissions permissions);

    Set<Edge> getEdgesBySource(VertexId source);

    Set<Edge> getEdgesByDestination(VertexId destination);

    Collection<Vertex> allVertices();

    Collection<Edge> allEdges();

    Collection<ZoneId> allZones();

    void serialize(OutputStream os);
}
