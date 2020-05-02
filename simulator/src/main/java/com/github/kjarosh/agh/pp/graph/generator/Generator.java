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
public class Generator {
    private final Random random = new Random();
    private final Faker faker = new Faker();

    private final Toml config;
    private final EntityGenerator entityGenerator;
    private final RelationGenerator relationGenerator;

    private final Map<ZoneAndType, List<VertexId>> verticesByZoneType = new HashMap<>();

    public Generator(Toml config) {
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
                r -= prob;
            } else {
                return entries.get(i).getKey();
            }
        }

        throw new AssertionError();
    }

    private ZoneId chooseZoneId() {
        int zoneCount = Math.toIntExact(config.getLong("count.zone"));
        return new ZoneId("zone" + random.nextInt(zoneCount));
    }

    public Graph generateGraph() {
        Graph graph = new Graph();

        int vertexCount = Math.toIntExact(config.getLong("count.entity"));
        int edgeCount = Math.toIntExact(config.getLong("count.relation"));

        graph.addVertex(entityGenerator.generateVertexWithType(Vertex.Type.PROVIDER));
        graph.addVertex(entityGenerator.generateVertexWithType(Vertex.Type.SPACE));
        graph.addVertex(entityGenerator.generateVertexWithType(Vertex.Type.GROUP));
        graph.addVertex(entityGenerator.generateVertexWithType(Vertex.Type.USER));

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
            return generateVertexWithType(type);
        }

        private Vertex generateVertexWithType(Vertex.Type type) {
            ZoneId zone = chooseZoneId();
            VertexId id = generateVertexId(type);
            return new Vertex(id, type, zone);
        }

        private Vertex.Type generateVertexType() {
            return chooseByShares(ImmutableMap.<Vertex.Type, Double>builder()
                    .put(Vertex.Type.PROVIDER,
                            config.getDouble("type.provider.share"))
                    .put(Vertex.Type.SPACE,
                            config.getDouble("type.space.share"))
                    .put(Vertex.Type.GROUP,
                            config.getDouble("type.group.share"))
                    .put(Vertex.Type.USER,
                            config.getDouble("type.user.share"))
                    .build());
        }

        private VertexId generateVertexId(Vertex.Type type) {
            switch (type) {
                case PROVIDER:
                    return new VertexId(assertUniqueId(faker.address().city()));
                case SPACE:
                    return new VertexId(assertUniqueId(faker.address().streetName()));
                case GROUP:
                    return new VertexId(assertUniqueId(faker.internet().slug()));
                case USER:
                    return new VertexId(assertUniqueId(faker.name().username()));
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
                    generatePermissions() : null;

            List<VertexId> fromSpace = verticesByZoneType.get(
                    new ZoneAndType(zoneA, relationType.from));
            List<VertexId> toSpace = verticesByZoneType.get(
                    new ZoneAndType(zoneB, relationType.to));

            int fromIx = random.nextInt(fromSpace.size());
            int toIx = random.nextInt(toSpace.size());
            VertexId from = fromSpace.get(fromIx);
            VertexId to = toSpace.get(toIx);

            if (relationType.from == relationType.to) {
                if (fromIx > toIx) {
                    VertexId tmp = from;
                    to = from;
                    from = tmp;
                }
            }

            return new Edge(from, to, permissions);
        }

        private RelationType generateRelationType() {
            return chooseByShares(ImmutableMap.<RelationType, Double>builder()
                    .put(RelationType.SPACE_PROVIDER,
                            config.getDouble("relation.space_provider.share"))
                    .put(RelationType.GROUP_SPACE,
                            config.getDouble("relation.group_space.share"))
                    .put(RelationType.GROUP_GROUP,
                            config.getDouble("relation.group_group.share"))
                    .put(RelationType.USER_GROUP,
                            config.getDouble("relation.user_group.share"))
                    .put(RelationType.USER_SPACE,
                            config.getDouble("relation.user_space.share"))
                    .build());
        }

        private Permissions generatePermissions() {
            StringBuilder value = new StringBuilder(5);
            for (int i = 0; i < 5; ++i) {
                value.append(random.nextBoolean() ? '0' : '1');
            }
            return new Permissions(value.toString());
        }

        private boolean isRelationInterZone() {
            double prob = config.getDouble("feature.inter_zone_relation.prob");
            return random.nextDouble() < prob;
        }
    }
}

