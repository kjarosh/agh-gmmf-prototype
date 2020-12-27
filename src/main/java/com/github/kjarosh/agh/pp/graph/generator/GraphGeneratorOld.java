package com.github.kjarosh.agh.pp.graph.generator;

import com.github.javafaker.Faker;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.google.common.collect.ImmutableMap;
import com.moandjiezana.toml.Toml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Kamil Jarosz
 */
@Deprecated
public class GraphGeneratorOld {
    private final Random random = new Random();
    private final Faker faker = new Faker();

    private final Toml config;
    private final EntityGenerator entityGenerator;
    private final RelationGenerator relationGenerator;

    private final Map<ZoneAndType, List<VertexId>> verticesByZoneType = new HashMap<>();

    public GraphGeneratorOld(Toml config) {
        this.config = config;
        this.entityGenerator = this.new EntityGenerator();
        this.relationGenerator = this.new RelationGenerator();
    }

    private <T> T chooseByShares(Map<T, Double> shares) {
        List<Map.Entry<T, Double>> entries = new ArrayList<>(shares.entrySet());
        double sharesSum = entries.stream()
                .mapToDouble(Map.Entry::getValue)
                .sum();
        List<Double> probabilities = entries.stream()
                .map(e -> e.getValue() / sharesSum)
                .collect(Collectors.toList());
        double r = random.nextDouble();
        for (int i = 0; i < entries.size(); ++i) {
            double prob = probabilities.get(i);
            if (r < prob) {
                return entries.get(i).getKey();
            } else {
                r -= prob;
            }
        }

        throw new AssertionError();
    }

    private ZoneId chooseZoneId() {
        int zoneCount = getZoneCount();
        return getZoneId(random.nextInt(zoneCount));
    }

    private ZoneId getZoneId(int i) {
        return new ZoneId("zone" + i);
    }

    private int getZoneCount() {
        return Math.toIntExact(config.getLong("count.zone"));
    }

    public Graph generateGraph() {
        Graph graph = new Graph();

        int vertexCount = Math.toIntExact(config.getLong("count.entity"));
        int edgeCount = Math.toIntExact(config.getLong("count.relation"));

        for (int i = 0; i < getZoneCount(); ++i) {
            ZoneId z = getZoneId(i);
            graph.addVertex(entityGenerator.generateVertex(z, Vertex.Type.PROVIDER));
            graph.addVertex(entityGenerator.generateVertex(z, Vertex.Type.SPACE));
            graph.addVertex(entityGenerator.generateVertex(z, Vertex.Type.GROUP));
            graph.addVertex(entityGenerator.generateVertex(z, Vertex.Type.USER));
        }

        for (int i = 0; i < vertexCount - 4; ++i) {
            graph.addVertex(entityGenerator.generateVertex());
        }

        for (int i = 0; i < edgeCount; ++i) {
            graph.addEdge(relationGenerator.generateEdge());
        }

        return graph;
    }

    private enum RelationType {
        SPACE_PROVIDER(Vertex.Type.SPACE, Vertex.Type.PROVIDER),
        GROUP_SPACE(Vertex.Type.GROUP, Vertex.Type.SPACE),
        GROUP_GROUP(Vertex.Type.GROUP, Vertex.Type.GROUP),
        USER_GROUP(Vertex.Type.USER, Vertex.Type.GROUP),
        USER_SPACE(Vertex.Type.USER, Vertex.Type.SPACE),
        ;

        private final Vertex.Type from;
        private final Vertex.Type to;

        RelationType(Vertex.Type from, Vertex.Type to) {
            this.from = from;
            this.to = to;
        }
    }

    private class EntityGenerator {
        private final Map<String, Integer> usedIds = new HashMap<>();

        private Vertex generateVertex() {
            Vertex.Type type = generateVertexType();
            ZoneId zone = chooseZoneId();
            return generateVertex(zone, type);
        }

        private Vertex generateVertex(ZoneId zone, Vertex.Type type) {
            VertexId id = generateVertexId(zone, type);
            Vertex v = new Vertex(id, type);

            verticesByZoneType.computeIfAbsent(
                    new ZoneAndType(v.id().owner(), v.type()), i -> new ArrayList<>())
                    .add(id);
            return v;
        }

        private Vertex.Type generateVertexType() {
            return chooseByShares(ImmutableMap.<Vertex.Type, Double>builder()
                    .put(Vertex.Type.PROVIDER,
                            config.getDouble("share.type.provider"))
                    .put(Vertex.Type.SPACE,
                            config.getDouble("share.type.space"))
                    .put(Vertex.Type.GROUP,
                            config.getDouble("share.type.group"))
                    .put(Vertex.Type.USER,
                            config.getDouble("share.type.user"))
                    .build());
        }

        private VertexId generateVertexId(ZoneId zone, Vertex.Type type) {
            switch (type) {
                case PROVIDER:
                    return new VertexId(zone, assertUniqueId(faker.address().city()));
                case SPACE:
                    return new VertexId(zone, assertUniqueId(faker.address().streetName()));
                case GROUP:
                    return new VertexId(zone, assertUniqueId(faker.internet().slug()));
                case USER:
                    return new VertexId(zone, assertUniqueId(faker.name().username()));
            }

            throw new AssertionError();
        }

        private String assertUniqueId(String id) {
            if (!usedIds.containsKey(id)) {
                usedIds.put(id, 0);
            }

            int number = usedIds.get(id);
            usedIds.put(id, number + 1);
            return number > 0 ? id + number : id;
        }
    }

    private class RelationGenerator {
        private final Set<VertexIdPair> generatedEdges = new HashSet<>();

        private Edge generateEdge() {
            Edge edge = null;
            while (edge == null) {
                edge = generateEdge0();
            }

            VertexIdPair pair = new VertexIdPair(edge.src(), edge.dst());
            if (generatedEdges.contains(pair)) {
                return generateEdge();
            }

            generatedEdges.add(pair);
            return edge;
        }

        private Edge generateEdge0() {
            RelationType relationType = generateRelationType();
            boolean interZone = isRelationInterZone();
            ZoneId zoneA = chooseZoneId();
            ZoneId zoneB = interZone ? chooseZoneId() : zoneA;
            Permissions permissions = relationType.to != Vertex.Type.PROVIDER ?
                    Permissions.random(random) : null;

            List<VertexId> fromSpace = verticesByZoneType.get(
                    new ZoneAndType(zoneA, relationType.from));
            List<VertexId> toSpace = verticesByZoneType.get(
                    new ZoneAndType(zoneB, relationType.to));

            int fromIx = random.nextInt(fromSpace.size());
            int toIx = random.nextInt(toSpace.size());
            if (fromIx == toIx) {
                return null;
            }

            VertexId from = fromSpace.get(fromIx);
            VertexId to = toSpace.get(toIx);

            if (relationType.from == relationType.to) {
                if (fromIx > toIx) {
                    VertexId tmp = to;
                    to = from;
                    from = tmp;
                }
            }

            if (from.equals(to)) {
                throw new RuntimeException();
            }

            return new Edge(from, to, permissions);
        }

        private RelationType generateRelationType() {
            return chooseByShares(ImmutableMap.<RelationType, Double>builder()
                    .put(RelationType.SPACE_PROVIDER,
                            config.getDouble("share.relation.space_provider"))
                    .put(RelationType.GROUP_SPACE,
                            config.getDouble("share.relation.group_space"))
                    .put(RelationType.GROUP_GROUP,
                            config.getDouble("share.relation.group_group"))
                    .put(RelationType.USER_GROUP,
                            config.getDouble("share.relation.user_group"))
                    .put(RelationType.USER_SPACE,
                            config.getDouble("share.relation.user_space"))
                    .build());
        }

        private boolean isRelationInterZone() {
            double prob = config.getDouble("prob.relation.inter_zone");
            return random.nextDouble() < prob;
        }
    }
}

