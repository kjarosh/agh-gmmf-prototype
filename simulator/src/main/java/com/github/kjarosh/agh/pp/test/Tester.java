package com.github.kjarosh.agh.pp.test;

import ch.qos.logback.classic.Level;
import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.ZoneClient;
import com.github.kjarosh.agh.pp.util.LoggerUtils;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private final boolean dynamicTests;
    private final Graph graph;

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
        this.dynamicTests = dynamicTests;
        this.graph = GraphLoader.loadGraph(graphPath);
    }

    public static void main(String zone, List<String> allZones) {
        LoggerUtils.setLoggingLevel("org.springframework.web", Level.INFO);

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

    private void buildGraph() {
        System.out.println("Building graph");
        new RemoteGraphBuilder(graph, client, allZones).build(client, zone);
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
        assertEqual(f.apply(vid("zone0:jill"), vid("zone1:krakow")),
                "00000");
        assertEqual(f.apply(vid("zone0:jill"), vid("zone0:paris")),
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
        assertEqual(f.apply(vid("zone0:luke"), vid("zone0:dhub_members")),
                "11111");
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
        int count = Optional.ofNullable(System.getenv("DYNAMIC_TESTS_COUNT"))
                .map(Integer::parseInt)
                .orElse(100);
        boolean parallel = "true".equals(System.getenv("DYNAMIC_TESTS_PARALLEL"));

        makeStream(count, parallel).forEach(i -> {
            Vertex from = getRandom(graph.allVertices());
            Vertex to = getRandom(graph.allVertices());

            String naive = client.naiveEffectivePermissions(
                    zone, from.id(), to.id());
            String indexed = client.indexedEffectivePermissions(
                    zone, from.id(), to.id());
            assertEqual(naive, indexed,
                    "testing permissions from " + from + ", to " + to);
        });
        Assert.Stats effectivePermissions = Assert.statistics.reset();

        makeStream(count, parallel).forEach(i -> {
            Vertex of = getRandom(graph.allVertices());

            List<String> naive = client.naiveMembers(zone, of.id());
            Collection<String> indexed = client.indexedMembers(zone, of.id());
            assertEqualSet(naive, indexed, "testing members of " + of.id());
        });
        Assert.Stats members = Assert.statistics.reset();

        System.out.println("Effective permissions: " + effectivePermissions);
        System.out.println("Members: " + members);
    }

    private IntStream makeStream(int count, boolean parallel) {
        IntStream stream = IntStream.range(0, count);
        return parallel ? stream.parallel() : stream;
    }

    private <E> E getRandom(Collection<E> e) {
        return e.stream()
                .skip((int) (e.size() * Math.random()))
                .findFirst()
                .orElseThrow(AssertionError::new);
    }
}
