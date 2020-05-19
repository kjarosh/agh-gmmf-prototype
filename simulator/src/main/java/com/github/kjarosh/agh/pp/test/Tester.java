package com.github.kjarosh.agh.pp.test;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.ZoneClient;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final List<ZoneId> allZones;
    private final String graphPath;
    private final boolean dynamicTests;

    public Tester(
            ZoneClient client,
            String zone,
            List<String> allZones,
            String graphPath,
            boolean dynamicTests) {
        this.client = client;
        this.zone = new ZoneId(zone);
        this.allZones = allZones.stream()
                .map(ZoneId::new)
                .collect(Collectors.toList());
        this.graphPath = graphPath;
        this.dynamicTests = dynamicTests;
    }

    public static void main(String zone, List<String> allZones) {
        boolean dynamicTests = "true".equals(System.getenv("DYNAMIC_TESTS"));
        String graphPath = System.getenv("GRAPH_PATH");
        new Tester(new ZoneClient(), zone, allZones, graphPath, dynamicTests).test();
    }

    private VertexId vid(String bob) {
        return new VertexId(bob);
    }

    @SneakyThrows
    private void test() {
        while (notHealthy()) {
            Thread.sleep(200);
        }

        long start = System.nanoTime();
        buildGraph();

        if (dynamicTests) {
            runDynamicTests();
            return;
        }

        testIsAdjacent();
        testListAdjacent();
        testPermissions();

        Assert.Stats basicStats = Assert.statistics.reset();

        testReaches((f, t) -> client.naiveReaches(zone, f, t));
        testMembers((o) -> client.naiveMembers(zone, o));
        testEffectivePermissions((f, t) -> client.naiveEffectivePermissions(zone, f, t));

        Assert.Stats naiveStats = Assert.statistics.reset();

        while (indexNotReady()) {
            Thread.sleep(200);
        }

        testReaches((f, t) -> client.indexedReaches(zone, f, t));
        testMembers((o) -> client.indexedMembers(zone, o));
        testEffectivePermissions((f, t) -> client.indexedEffectivePermissions(zone, f, t));

        Assert.Stats indexedStats = Assert.statistics.reset();

        long time = System.nanoTime() - start;

        System.out.println();
        System.out.println("Basic: " + basicStats);
        System.out.println("Naive: " + naiveStats);
        System.out.println("Indexed: " + indexedStats);
        System.out.println("Time: " + time / 1_000_000_000D + " s");
    }

    private boolean notHealthy() {
        return allZones.stream()
                .anyMatch(zone -> !client.healthcheck(zone));
    }

    private boolean indexNotReady() {
        return allZones.stream()
                .anyMatch(zone -> !client.indexReady(zone));
    }

    private void buildGraph() {
        System.out.println("Building graph");

        Graph model = GraphLoader.loadGraph(graphPath);
        model.allVertices().forEach(v -> {
            client.addVertex(v.id(), v.type());
        });
        model.allEdges()
                .stream()
                .sorted(Comparator.comparing(Edge::src)
                        .thenComparing(Edge::dst))
                .forEach(e -> {
                    client.addEdge(zone, e.src(), e.dst(), e.permissions());
                });

        System.out.println("Graph built");
    }

    private void testIsAdjacent() {
        assertTrue(client.isAdjacent(zone, vid("zone0:bob"), vid("zone0:datahub")));
        assertFalse(client.isAdjacent(zone, vid("zone0:bob"), vid("zone0:alice")));
    }

    private void testListAdjacent() {
        assertEqualSet(client.listAdjacent(zone, vid("zone0:uber_admins")),
                Collections.singletonList("zone1:admins"));
        assertEqualSet(client.listAdjacent(zone, vid("zone1:anne")),
                Arrays.asList("zone0:ceric", "zone1:audit", "zone1:members"));
        assertEqualSet(client.listAdjacent(zone, vid("zone1:krakow")),
                Collections.emptyList());

        assertEqualSet(client.listAdjacentReversed(zone, vid("zone1:anne")),
                Collections.emptyList());
        assertEqualSet(client.listAdjacentReversed(zone, vid("zone0:paris")),
                Arrays.asList("zone0:datahub", "zone0:eo_data", "zone1:eosc"));
    }

    private void testPermissions() {
        assertEqual(client.permissions(zone, vid("zone0:alice"), vid("zone0:bob")),
                null);
        assertEqual(client.permissions(zone, vid("zone0:alice"), vid("zone0:ebi")),
                "11000");
        assertEqual(client.permissions(zone, vid("zone1:audit"), vid("zone1:cyfnet")),
                "11001");
        assertEqual(client.permissions(zone, vid("zone1:audit"), vid("zone1:eosc")),
                null);
    }

    private void testReaches(BiFunction<VertexId, VertexId, Boolean> f) {
        assertTrue(f.apply(vid("zone0:bob"), vid("zone0:datahub")));
        assertFalse(f.apply(vid("zone0:bob"), vid("zone1:admins")));
        assertTrue(f.apply(vid("zone0:bob"), vid("zone0:dhub_members")));
        assertTrue(f.apply(vid("zone0:luke"), vid("zone1:krakow")));
        assertFalse(f.apply(vid("zone1:anne"), vid("zone0:lisbon")));
        assertTrue(f.apply(vid("zone0:luke"), vid("zone0:dhub_members")));
        assertTrue(f.apply(vid("zone0:jill"), vid("zone1:krakow")));
    }

    private void testMembers(Function<VertexId, Collection<String>> f) {
        assertEqualSet(f.apply(vid("zone1:admins")), Arrays.asList(
                "zone0:uber_admins",
                "zone0:luke"));

        assertEqualSet(f.apply(vid("zone0:eo_data")), Arrays.asList(
                "zone0:dhub_mngrs",
                "zone0:dhub_members",
                "zone0:uber_admins",
                "zone1:admins",
                "zone0:alice",
                "zone0:bob",
                "zone0:luke"));

        assertEqualSet(f.apply(vid("zone0:ebi")), Arrays.asList(
                "zone0:alice",
                "zone0:jill"));

        assertEqualSet(f.apply(vid("zone1:cyfnet")), Arrays.asList(
                "zone1:audit",
                "zone1:members",
                "zone1:tom",
                "zone1:anne",
                "zone1:admins",
                "zone0:uber_admins",
                "zone0:luke"));

        assertEqualSet(f.apply(vid("zone1:krakow")), Arrays.asList(
                "zone1:eosc",
                "zone1:primage",
                "zone1:members",
                "zone1:tom",
                "zone1:anne",
                "zone1:audit",
                "zone1:admins",
                "zone1:cyfnet",
                "zone0:uber_admins",
                "zone0:luke",
                "zone0:ceric",
                "zone0:ebi",
                "zone0:jill",
                "zone0:alice",
                "zone0:eo_data",
                "zone0:dhub_members",
                "zone0:dhub_mngrs",
                "zone0:bob"));
    }

    private void testEffectivePermissions(BiFunction<VertexId, VertexId, String> f) {
        assertEqual(f.apply(vid("zone0:alice"), vid("zone0:bob")),
                null);
        assertEqual(f.apply(vid("zone0:alice"), vid("zone0:ebi")),
                "11000");
        assertEqual(f.apply(vid("zone1:audit"), vid("zone1:cyfnet")),
                "11001");
        assertEqual(f.apply(vid("zone1:audit"), vid("zone1:eosc")),
                "11001");
        assertEqual(f.apply(vid("zone1:tom"), vid("zone1:primage")),
                "11011");
        assertEqual(f.apply(vid("zone0:luke"), vid("zone1:eosc")),
                "11011");
        assertEqual(f.apply(vid("zone1:anne"), vid("zone1:cyfnet")),
                "11001");
//        assertEqual(f.apply(vid("zone0:luke"), vid("zone0:dhub_members")),
//                "11111");
        assertEqual(f.apply(vid("zone1:admins"), vid("zone1:eosc")),
                "11011");
        assertEqual(f.apply(vid("zone0:ebi"), vid("zone0:ceric")),
                "10000");
        assertEqual(f.apply(vid("zone0:bob"), vid("zone0:datahub")),
                "11111");
        assertEqual(f.apply(vid("zone1:tom"), vid("zone1:eosc")),
                "11001");
    }

    private void runDynamicTests() {
        // TODO
    }
}
