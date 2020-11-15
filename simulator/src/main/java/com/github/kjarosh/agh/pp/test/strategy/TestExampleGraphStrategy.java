package com.github.kjarosh.agh.pp.test.strategy;

import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.test.Assert;
import com.google.common.collect.Sets;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.kjarosh.agh.pp.test.Assert.assertEqual;
import static com.github.kjarosh.agh.pp.test.Assert.assertEqualSet;
import static com.github.kjarosh.agh.pp.test.Assert.assertFalse;
import static com.github.kjarosh.agh.pp.test.Assert.assertTrue;

/**
 * @author Kamil Jarosz
 */
public class TestExampleGraphStrategy implements TestStrategy {
    private final String graphPath;

    public TestExampleGraphStrategy(String graphPath) {
        this.graphPath = graphPath;
    }

    private VertexId vid(String bob) {
        return new VertexId(bob);
    }

    private EdgeId eid(String from, String to) {
        return new EdgeId(new VertexId(from), new VertexId(to));
    }

    @Override
    public void execute(TestContext context) {
        ZoneClient client = context.getClient();
        ZoneId zone = context.getZone();

        long start = System.nanoTime();

        context.buildGraph(graphPath);

        testIsAdjacent(context);
        testListAdjacent(context);
        testPermissions(context);

        Assert.Stats basicStats = Assert.statistics.reset();

        Map<String, Assert.Stats> naiveByType = new HashMap<>();
        Map<String, Assert.Stats> indexedByType = new HashMap<>();

        testReaches(e -> client.naive().reaches(zone, e));
        naiveByType.put("reaches", Assert.statistics.reset());
        testMembers((o) -> client.naive().members(zone, o));
        naiveByType.put("members", Assert.statistics.reset());
        testEffectivePermissions(e -> client.naive().effectivePermissions(zone, e));
        naiveByType.put("eperms", Assert.statistics.reset());

        testReaches(e -> client.indexed().reaches(zone, e));
        indexedByType.put("reaches", Assert.statistics.reset());
        testMembers((o) -> client.indexed().members(zone, o));
        indexedByType.put("members", Assert.statistics.reset());
        testEffectivePermissions(e -> client.indexed().effectivePermissions(zone, e));
        indexedByType.put("eperms", Assert.statistics.reset());

        modifyPermissions(context);
        testPermissionsAfterModification(e -> client.naive().effectivePermissions(zone, e));
        naiveByType.put("perm mod", Assert.statistics.reset());
        testPermissionsAfterModification(e -> client.indexed().effectivePermissions(zone, e));
        indexedByType.put("perm mod", Assert.statistics.reset());
        testMembersAfterModification(e -> client.naive().members(zone, e));
        naiveByType.put("members mod", Assert.statistics.reset());
        testMembersAfterModification(e -> client.indexed().members(zone, e));
        indexedByType.put("members mod", Assert.statistics.reset());

        long time = System.nanoTime() - start;
        System.out.println();

        Assert.Stats totalStats = Stream.of(
                naiveByType.values().stream(),
                indexedByType.values().stream(),
                Stream.of(basicStats))
                .flatMap(Function.identity())
                .reduce(Assert.Stats::reduce)
                .orElseThrow();

        Table results = Table.create("Failed tests");
        results.addColumns(StringColumn.create(""));
        results.addColumns(Sets.union(naiveByType.keySet(), indexedByType.keySet()).stream()
                .map(StringColumn::create)
                .toArray(Column[]::new));
        Row naiveRow = results.appendRow();
        naiveRow.setString("", "naive");
        naiveByType.forEach((type, stats) -> naiveRow.setString(type, "" + stats.failed()));
        Row indexedRow = results.appendRow();
        indexedRow.setString("", "indexed");
        indexedByType.forEach((type, stats) -> indexedRow.setString(type, "" + stats.failed()));
        System.out.println(results);
        System.out.println();
        System.out.println("Basic: " + basicStats);
        System.out.println("Total: " + totalStats);
        System.out.println("Time: " + time / 1_000_000_000D + " s");
    }


    private void testIsAdjacent(TestContext context) {
        ZoneClient client = context.getClient();
        ZoneId zone = context.getZone();

        assertTrue(client.isAdjacent(zone, eid("zone0:bob", "zone0:datahub")));
        assertFalse(client.isAdjacent(zone, eid("zone0:bob", "zone0:alice")));
    }

    private void testListAdjacent(TestContext context) {
        ZoneClient client = context.getClient();
        ZoneId zone = context.getZone();

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

    private void testPermissions(TestContext context) {
        ZoneClient client = context.getClient();
        ZoneId zone = context.getZone();

        assertEqual(client.permissions(zone, eid("zone0:alice", "zone0:bob")),
                null);
        assertEqual(client.permissions(zone, eid("zone0:alice", "zone0:ebi")),
                "11000");
        assertEqual(client.permissions(zone, eid("zone1:audit", "zone1:cyfnet")),
                "11001");
        assertEqual(client.permissions(zone, eid("zone1:audit", "zone1:eosc")),
                null);
    }

    private void testReaches(Function<EdgeId, Boolean> f) {
        assertTrue(f.apply(eid("zone0:bob", "zone0:datahub")));
        assertFalse(f.apply(eid("zone0:bob", "zone1:admins")));
        assertTrue(f.apply(eid("zone0:bob", "zone0:dhub_members")));
        assertTrue(f.apply(eid("zone0:luke", "zone1:krakow")));
        assertFalse(f.apply(eid("zone1:anne", "zone0:lisbon")));
        assertTrue(f.apply(eid("zone0:luke", "zone0:dhub_members")));
        assertTrue(f.apply(eid("zone0:jill", "zone1:krakow")));
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

    private void testEffectivePermissions(Function<EdgeId, String> f) {
        assertEqual(f.apply(eid("zone0:alice", "zone0:bob")),
                null);
        assertEqual(f.apply(eid("zone0:jill", "zone1:krakow")),
                "00000");
        assertEqual(f.apply(eid("zone0:jill", "zone0:paris")),
                null);
        assertEqual(f.apply(eid("zone0:alice", "zone0:ebi")),
                "11000");
        assertEqual(f.apply(eid("zone1:audit", "zone1:cyfnet")),
                "11001");
        assertEqual(f.apply(eid("zone1:audit", "zone1:eosc")),
                "11001");
        assertEqual(f.apply(eid("zone1:tom", "zone1:primage")),
                "11011");
        assertEqual(f.apply(eid("zone0:luke", "zone1:eosc")),
                "11011");
        assertEqual(f.apply(eid("zone1:anne", "zone1:cyfnet")),
                "11001");
        assertEqual(f.apply(eid("zone0:luke", "zone0:dhub_members")),
                "11111");
        assertEqual(f.apply(eid("zone1:admins", "zone1:eosc")),
                "11011");
        assertEqual(f.apply(eid("zone0:ebi", "zone0:ceric")),
                "10000");
        assertEqual(f.apply(eid("zone0:bob", "zone0:datahub")),
                "11111");
        assertEqual(f.apply(eid("zone1:tom", "zone1:eosc")),
                "11001");
        assertEqual(f.apply(eid("zone0:alice", "zone0:ceric")),
                "10000");
    }

    private void modifyPermissions(TestContext context) {
        ZoneClient client = context.getClient();
        ZoneId zone = context.getZone();

        client.setPermissions(zone,
                eid("zone1:tom", "zone1:primage"),
                new Permissions("11001"));
        client.setPermissions(zone,
                eid("zone0:uber_admins", "zone1:admins"),
                new Permissions("10100"));
        client.setPermissions(zone,
                eid("zone1:admins", "zone1:eosc"),
                new Permissions("10000"));
        client.setPermissions(zone,
                eid("zone1:cyfnet", "zone1:eosc"),
                new Permissions("00001"));

        client.removeEdge(zone,
                eid("zone0:alice", "zone0:ebi"));
    }

    private void testPermissionsAfterModification(Function<EdgeId, String> f) {
        assertEqual(f.apply(eid("zone1:tom", "zone1:primage")),
                "11001");
        assertEqual(f.apply(eid("zone0:luke", "zone1:admins")),
                "10100");
        assertEqual(f.apply(eid("zone0:luke", "zone1:eosc")),
                "10001");
        assertEqual(f.apply(eid("zone0:alice", "zone0:ceric")),
                null);
        assertEqual(f.apply(eid("zone0:alice", "zone0:ebi")),
                null);
        assertEqual(f.apply(eid("zone0:alice", "zone1:krakow")),
                "00000");
    }

    private void testMembersAfterModification(Function<VertexId, Collection<String>> f) {
        assertEqualSet(f.apply(vid("zone0:ebi")), Arrays.asList(
                "zone0:jill"));

        assertEqualSet(f.apply(vid("zone0:ceric")), Arrays.asList(
                "zone0:ebi",
                "zone1:anne",
                "zone0:jill"));

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
}
