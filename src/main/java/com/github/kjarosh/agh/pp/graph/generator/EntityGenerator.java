package com.github.kjarosh.agh.pp.graph.generator;

import com.github.javafaker.Faker;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author Kamil Jarosz
 */
public class EntityGenerator {
    private final Faker faker = new Faker();
    private final Map<String, Integer> usedIds = new HashMap<>();

    public List<Vertex> generateVertices(Supplier<ZoneId> zoneSupplier, int count, Vertex.Type type) {
        List<Vertex> ret = new ArrayList<>();
        for(int i = 0; i < count; ++i){
            ret.add(generateVertex(zoneSupplier.get(), type));
        }
        return ret;
    }

    public Vertex generateVertex(ZoneId zone, Vertex.Type type) {
        VertexId id = generateVertexId(zone, type);
        return new Vertex(id, type);
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
