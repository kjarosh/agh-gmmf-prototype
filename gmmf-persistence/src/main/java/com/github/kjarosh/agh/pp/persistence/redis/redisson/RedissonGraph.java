package com.github.kjarosh.agh.pp.persistence.redis.redisson;

import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.persistence.redis.RedisGraph;
import org.redisson.api.RBatch;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Kamil Jarosz
 */
public class RedissonGraph extends RedisGraph {
    private final ZoneId currentZoneId;
    private final RedissonClient redisson;

    public RedissonGraph(ZoneId currentZoneId, RedissonClient redisson, String prefix) {
        super(prefix);
        this.currentZoneId = currentZoneId;
        this.redisson = redisson;
    }

    @Override
    public void addVertex(Vertex v) {
        if (currentZoneId != null && !v.id().owner().equals(currentZoneId)) {
            throw new IllegalStateException();
        }

        RBatch batch = redisson.createBatch();
        batch.getBucket(keyVertex(v.id()), Codecs.VERTEX_TYPE).setAsync(v.type());
        batch.getSet(keyZones(), Codecs.ZONE_ID).addAsync(v.id().owner());
        batch.execute();
    }

    @Override
    public boolean hasVertex(VertexId id) {
        return redisson.getBucket(keyVertex(id)).isExists();
    }

    @Override
    public boolean hasEdge(EdgeId edgeId) {
        return redisson.getBucket(keyEdge(edgeId)).isExists();
    }

    @Override
    public Vertex getVertex(VertexId id) {
        Vertex.Type type = (Vertex.Type) redisson.getBucket(keyVertex(id), Codecs.VERTEX_TYPE).get();
        if (type == null) {
            return null;
        }
        return new Vertex(id, type);
    }

    @Override
    public void addEdge(Edge e) {
        RBatch batch = redisson.createBatch();
        batch.getSet(keyZones(), Codecs.ZONE_ID).addAsync(e.src().owner());
        batch.getSet(keyZones(), Codecs.ZONE_ID).addAsync(e.dst().owner());
        batch.getBucket(keyEdge(e.id()), Codecs.PERMISSIONS).setAsync(e.permissions());
        batch.getSet(keyEdgeByDst(e.dst()), Codecs.VERTEX_ID).addAsync(e.src());
        batch.getSet(keyEdgeBySrc(e.src()), Codecs.VERTEX_ID).addAsync(e.dst());
        batch.execute();
    }

    @Override
    public void removeEdge(Edge e) {
        RBatch batch = redisson.createBatch();
        batch.getBucket(keyEdge(e.id()), Codecs.PERMISSIONS).deleteAsync();
        batch.getSet(keyEdgeByDst(e.dst()), Codecs.VERTEX_ID).removeAsync(e.src());
        batch.getSet(keyEdgeBySrc(e.src()), Codecs.VERTEX_ID).removeAsync(e.dst());
        batch.execute();
    }

    @Override
    public Edge getEdge(EdgeId edgeId) {
        Permissions permissions = (Permissions) redisson.getBucket(keyEdge(edgeId), Codecs.PERMISSIONS).get();
        if (permissions == null) {
            return null;
        }
        return new Edge(edgeId.getFrom(), edgeId.getTo(), permissions);
    }

    @Override
    public void setPermissions(EdgeId edgeId, Permissions permissions) {
        redisson.getBucket(keyEdge(edgeId), Codecs.PERMISSIONS).set(permissions);
    }

    private Set<Edge> getEdgesFromKeys(String[] keys) {
        Map<String, Permissions> edges = redisson.getBuckets(Codecs.PERMISSIONS).get(keys);
        return edges.entrySet()
                .stream()
                .map(e -> {
                    String[] vx = parseEdgeFromKey(e.getKey());
                    return new Edge(new VertexId(vx[0]), new VertexId(vx[1]), e.getValue());
                })
                .collect(Collectors.toSet());
    }

    @Override
    public RSet<VertexId> getDestinationsBySource(VertexId source) {
        return redisson.getSet(keyEdgeBySrc(source), Codecs.VERTEX_ID);
    }

    @Override
    public RSet<VertexId> getSourcesByDestination(VertexId destination) {
        return redisson.getSet(keyEdgeByDst(destination), Codecs.VERTEX_ID);
    }

    @Override
    public Set<Edge> getEdgesBySource(VertexId source) {
        Set<VertexId> destinations = new HashSet<>(getDestinationsBySource(source));
        String[] keys = destinations.stream()
                .map(dst -> keyEdge(source.toString(), dst.toString()))
                .toArray(String[]::new);
        return getEdgesFromKeys(keys);
    }

    @Override
    public Set<Edge> getEdgesByDestination(VertexId destination) {
        Set<VertexId> sources = new HashSet<>(getSourcesByDestination(destination));
        String[] keys = sources.stream()
                .map(src -> keyEdge(src.toString(), destination.toString()))
                .toArray(String[]::new);
        return getEdgesFromKeys(keys);
    }

    @Override
    public Collection<ZoneId> allZones() {
        return redisson.getSet(keyZones(), Codecs.ZONE_ID);
    }
}
