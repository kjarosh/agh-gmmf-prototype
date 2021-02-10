package com.github.kjarosh.agh.pp.redis.lettuce;

import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.memory.InMemoryGraph;
import com.github.kjarosh.agh.pp.redis.RedisGraph;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.KeyValue;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.SneakyThrows;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.kjarosh.agh.pp.config.Config.ZONE_ID;

/**
 * @author Kamil Jarosz
 */
public class LettuceGraph extends RedisGraph {
    private final Graph backup = new InMemoryGraph();
    private final LettuceConnections lettuce;

    public LettuceGraph(LettuceConnections lettuce, String prefix) {
        super(prefix);
        this.lettuce = lettuce;
    }

    private void addZone(ZoneId zone) {
        lettuce.string().sync().sadd(keyZones(), zone.toString());
    }

    @SneakyThrows
    @Override
    public void addVertex(Vertex v) {
        backup.addVertex(v);

        if (ZONE_ID != null && !v.id().owner().equals(ZONE_ID)) {
            throw new IllegalStateException();
        }
        RedisCommands<String, String> commands = lettuce.string().sync();
        commands.set(keyVertex(v.id()), v.type().toString());
        addZone(v.id().owner());
    }

    @Override
    public boolean hasVertex(VertexId id) {
        return lettuce.string().sync().exists(keyVertex(id)) == 1;
    }

    @Override
    public boolean hasEdge(EdgeId edgeId) {
        return lettuce.permissions().sync().exists(keyEdge(edgeId)) == 1;
    }

    @Override
    public Vertex getVertex(VertexId id) {
        Vertex.Type type = Vertex.Type.valueOf(
                lettuce.string().sync().get(keyVertex(id)));
        return new Vertex(id, type);
    }

    @Override
    public void addEdge(Edge e) {
        backup.addEdge(e);

        lettuce.permissions().sync().set(keyEdge(e.id()), e.permissions());
        addZone(e.src().owner());
        addZone(e.dst().owner());
    }

    @Override
    public void removeEdge(Edge e) {
        backup.removeEdge(e);

        lettuce.permissions().sync().del(keyEdge(e.id()));
    }

    @Override
    public Edge getEdge(EdgeId edgeId) {
        Permissions permissions = lettuce.permissions().sync().get(keyEdge(edgeId));
        if (permissions == null) {
            return null;
        }
        return new Edge(edgeId.getFrom(), edgeId.getTo(), permissions);
    }

    @Override
    public void setPermissions(EdgeId edgeId, Permissions permissions) {
        backup.setPermissions(edgeId, permissions);

        lettuce.permissions().sync().set(keyEdge(edgeId), permissions);
    }

    @Override
    protected Stream<EdgeId> getEdgeIdsByKeyPattern(String keyPattern) {
        ScanArgs args = ScanArgs.Builder.matches(keyPattern).limit(5000);
        RedisCommands<String, Permissions> commands = lettuce.permissions().sync();
        Set<EdgeId> edges = new HashSet<>();

        ScanCursor cursor = ScanCursor.INITIAL;
        while (!cursor.isFinished()) {
            cursor = commands.scan(key -> {
                String[] vx = parseEdgeFromKey(key);
                VertexId from = new VertexId(vx[0]);
                VertexId to = new VertexId(vx[1]);
                edges.add(new EdgeId(from, to));
            }, cursor, args);
        }

        return edges.stream();
    }

    @Override
    protected Set<Edge> getEdgesByKeyPattern(String keyPattern) {
        ScanArgs args = ScanArgs.Builder.matches(keyPattern).limit(5000);
        RedisCommands<String, Permissions> commands = lettuce.permissions().sync();
        Set<Edge> edges = new HashSet<>();

        KeyScanCursor<String> cursor = commands.scan(args);
        while (true) {
            List<String> keys = cursor.getKeys();
            if (!keys.isEmpty()) {
                List<KeyValue<String, Permissions>> entries = commands.mget(
                        keys.toArray(new String[0]));
                entries.forEach(e -> {
                    if (e.getKey() == null || e.getValue() == null) {
                        return;
                    }
                    String[] vx = parseEdgeFromKey(e.getKey());
                    VertexId from = new VertexId(vx[0]);
                    VertexId to = new VertexId(vx[1]);
                    edges.add(new Edge(from, to, e.getValue()));
                });
            }
            if (cursor.isFinished()) {
                break;
            }
            cursor = commands.scan(cursor, args);
        }

        return edges;
    }

    @Override
    public Collection<Vertex> allVertices() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Edge> allEdges() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<ZoneId> allZones() {
        return lettuce.string().sync()
                .smembers(keyZones())
                .stream()
                .map(ZoneId::new)
                .collect(Collectors.toList());
    }
}
