package com.github.kjarosh.agh.pp.test;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.ZoneClient;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.github.kjarosh.agh.pp.test.Assert.assertEqual;
import static com.github.kjarosh.agh.pp.test.Assert.assertEqualSet;
import static com.github.kjarosh.agh.pp.test.Assert.assertFalse;
import static com.github.kjarosh.agh.pp.test.Assert.assertTrue;

/**
 * @author Kamil Jarosz
 */
public class Tester {
    private final ZoneClient client;
    private final ZoneId zone;

    public Tester(ZoneClient client, String zone) {
        this.client = client;
        this.zone = new ZoneId(zone);
    }

    public static void main(String[] args) {
        if (args.length != 2)
            throw new RuntimeException("Got " + args.length + " args, expected 2");

        new Tester(new ZoneClient(), args[1]).test();

        System.out.println("Failed assertions: " + Assert.failedAssertions);
    }

    private VertexId vid(String bob) {
        return new VertexId(bob);
    }

    @SneakyThrows
    private void test() {
        while (!client.healthcheck(zone)) {
            Thread.sleep(200);
        }

        buildGraph();

        testIsAdjacent();
        testListAdjacent();
        testPermissions();

        testReaches((f, t) -> client.naiveReaches(zone, f, t));
        testMembers((o) -> client.naiveMembers(zone, o));
        testEffectivePermissions((f, t) -> client.naiveEffectivePermissions(zone, f, t));

        testReaches((f, t) -> client.indexedReaches(zone, f, t));
        testMembers((o) -> client.indexedMembers(zone, o));
        testEffectivePermissions((f, t) -> client.indexedEffectivePermissions(zone, f, t));
    }

    private void buildGraph() {
        System.out.println("Building graph");

        Graph model = GraphLoader.loadGraph("graph.json");
        model.allVertices().forEach(v -> {
            client.addVertex(v.zone(), v.id(), v.type());
        });
        model.allEdges().forEach(e -> {
            client.addEdge(zone, e.src(), e.dst(), e.permissions());
        });

        System.out.println("Graph built");
    }

    private void testIsAdjacent() {
        assertTrue(client.isAdjacent(zone, vid("bob"), vid("datahub")));
        assertFalse(client.isAdjacent(zone, vid("bob"), vid("alice")));
    }

    private void testListAdjacent() {
        assertEqualSet(client.listAdjacent(zone, vid("uber_admins")),
                Collections.singletonList("admins"));
        assertEqualSet(client.listAdjacent(zone, vid("anne")),
                Arrays.asList("ceric", "audit", "members"));
        assertEqualSet(client.listAdjacent(zone, vid("krakow")),
                Collections.emptyList());

        assertEqualSet(client.listAdjacentReversed(zone, vid("anne")),
                Collections.emptyList());
        assertEqualSet(client.listAdjacentReversed(zone, vid("paris")),
                Arrays.asList("datahub", "eo_data", "eosc"));
    }

    private void testPermissions() {
        assertEqual(client.permissions(zone, vid("alice"), vid("bob")),
                null);
        assertEqual(client.permissions(zone, vid("alice"), vid("ebi")),
                "11000");
        assertEqual(client.permissions(zone, vid("audit"), vid("cyfnet")),
                "11001");
        assertEqual(client.permissions(zone, vid("audit"), vid("eosc")),
                null);
    }

    private void testReaches(BiFunction<VertexId, VertexId, Boolean> f) {
        assertTrue(f.apply(vid("bob"), vid("datahub")));
        assertFalse(f.apply(vid("bob"), vid("alice")));
        assertTrue(f.apply(vid("bob"), vid("dhub_members")));
        assertTrue(f.apply(vid("luke"), vid("krakow")));
        assertFalse(f.apply(vid("anne"), vid("lisbon")));
        assertTrue(f.apply(vid("luke"), vid("dhub_members")));
    }

    private void testMembers(Function<VertexId, Collection<String>> f) {
        assertEqualSet(f.apply(vid("admins")),
                Collections.singletonList("luke"));
        assertEqualSet(f.apply(vid("eo_data")),
                Arrays.asList("luke", "bob", "alice"));
    }

    private void testEffectivePermissions(BiFunction<VertexId, VertexId, String> f) {
        assertEqual(f.apply(vid("alice"), vid("bob")),
                null);
        assertEqual(f.apply(vid("alice"), vid("ebi")),
                "11000");
        assertEqual(f.apply(vid("audit"), vid("cyfnet")),
                "11001");
        assertEqual(f.apply(vid("audit"), vid("eosc")),
                "11001");
        assertEqual(f.apply(vid("tom"), vid("primage")),
                "11011");
    }
}
