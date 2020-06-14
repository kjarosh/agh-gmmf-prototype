package com.github.kjarosh.agh.pp.test.strategy;

import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.ZoneClient;
import com.github.kjarosh.agh.pp.test.Assert;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.github.kjarosh.agh.pp.test.Assert.assertEqual;
import static com.github.kjarosh.agh.pp.test.Assert.assertEqualSet;

/**
 * @author Kamil Jarosz
 */
public class DynamicTestsStrategy implements TestStrategy {
    private final String graphPath;

    public DynamicTestsStrategy(String graphPath) {
        this.graphPath = graphPath;
    }

    @Override
    public void execute(TestContext context) {
        ZoneClient client = context.getClient();
        ZoneId zone = context.getZone();
        Graph graph = context.buildGraph(graphPath);

        int count = Optional.ofNullable(System.getenv("DYNAMIC_TESTS_COUNT"))
                .map(Integer::parseInt)
                .orElse(100);
        boolean parallel = "true".equals(System.getenv("DYNAMIC_TESTS_PARALLEL"));

        AtomicInteger allPerms = new AtomicInteger(0);
        AtomicInteger nonReachablePerms = new AtomicInteger(0);
        makeStream(count, parallel).forEach(i -> {
            Vertex from;
            Vertex to;
            String indexed;
            boolean reaches;

            do {
                from = getRandom(graph.allVertices());
                to = getRandom(graph.allVertices());

                indexed = client.indexedEffectivePermissions(
                        zone, EdgeId.of(from.id(), to.id()));
                reaches = indexed != null;
            } while (!reaches && nonReachablePerms.get() * 2 >= allPerms.get());

            allPerms.incrementAndGet();
            if (!reaches) {
                nonReachablePerms.incrementAndGet();
            }

            String naive = client.naiveEffectivePermissions(
                    zone, EdgeId.of(from.id(), to.id()));
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
