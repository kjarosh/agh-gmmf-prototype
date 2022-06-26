package com.github.kjarosh.agh.pp.test;

import com.github.kjarosh.agh.pp.config.Config;
import com.github.kjarosh.agh.pp.config.ZoneConfig;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.test.util.IntegrationTestBase;
import com.github.kjarosh.agh.pp.test.util.SimulatorContainer;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Kamil Jarosz
 */
public class IntegrationTest extends IntegrationTestBase {
    private SimulatorContainer zone0;

    @Override
    protected String getGraphPath() {
        return null;
    }

    @SneakyThrows
    @Override
    protected void startEnvironment() {
        String redis = System.getProperty("test.redis", "false");

        Network network = Network.newNetwork();

        zone0 = new SimulatorContainer("zone0")
                .withEnv("REDIS", redis)
                .withNetwork(network);

        zone0.start();
    }

    @SneakyThrows
    @Override
    protected void stopEnvironment() {
        if (zone0 != null) {
            zone0.stop();
        }
    }

    @Override
    protected Config createConfiguration() {
        Config testConfig = new Config();
        testConfig.setZones(new HashMap<>());
        testConfig.getZones().put("zone0", ZoneConfig.builder()
                .address(getHostPort(zone0))
                .build());
        return testConfig;
    }

    private String getHostPort(SimulatorContainer container) {
        return container.getHost() + ":" + container.getMappedPort(80);
    }

    @Test
    void testCycle() {
        VertexId a = new VertexId("zone0:testCycle-a");
        VertexId b = new VertexId("zone0:testCycle-b");
        VertexId c = new VertexId("zone0:testCycle-c");

        Permissions p1 = new Permissions("10000");
        Permissions p2 = new Permissions("01000");
        Permissions p3 = new Permissions("00100");

        client.addVertex(a, Vertex.Type.GROUP);
        client.addVertex(b, Vertex.Type.GROUP);
        client.addVertex(c, Vertex.Type.GROUP);

        client.addEdge(zone, EdgeId.of(a, b), p1, "testCycle-1");
        client.addEdge(zone, EdgeId.of(b, c), p2, "testCycle-2");

        waitForIndex();

        assertThat(effectivePermissions(a, b)).isEqualTo(p1.toString());
        assertThat(effectivePermissions(b, c)).isEqualTo(p2.toString());
        assertThat(effectivePermissions(a, c)).isEqualTo(p2.toString());
        assertThat(effectivePermissions(c, a)).isNull();

        assertThat(members(c)).containsExactlyInAnyOrder(a.toString(), b.toString());
        assertThat(members(b)).containsExactlyInAnyOrder(a.toString());
        assertThat(members(a)).isEmpty();

        client.addEdge(zone, EdgeId.of(c, a), p3, "testCycle-3");
        waitForIndex();

        assertThat(effectivePermissions(a, b)).isEqualTo(p1.toString());
        assertThat(effectivePermissions(b, b)).isEqualTo(p1.toString());
        assertThat(effectivePermissions(c, b)).isEqualTo(p1.toString());

        assertThat(effectivePermissions(a, c)).isEqualTo(p2.toString());
        assertThat(effectivePermissions(b, c)).isEqualTo(p2.toString());
        assertThat(effectivePermissions(c, c)).isEqualTo(p2.toString());

        assertThat(effectivePermissions(a, a)).isEqualTo(p3.toString());
        assertThat(effectivePermissions(b, a)).isEqualTo(p3.toString());
        assertThat(effectivePermissions(c, a)).isEqualTo(p3.toString());

        assertThat(members(a))
                .containsExactlyInAnyOrder(a.toString(), b.toString(), c.toString());
        assertThat(members(b))
                .containsExactlyInAnyOrder(a.toString(), b.toString(), c.toString());
        assertThat(members(c))
                .containsExactlyInAnyOrder(a.toString(), b.toString(), c.toString());
    }

    private List<String> members(VertexId of) {
        return client.indexed().members(zone, of)
                .getMembers();
    }

    private String effectivePermissions(VertexId from, VertexId to) {
        return client.indexed().effectivePermissions(zone, EdgeId.of(from, to))
                .getEffectivePermissions();
    }

    @Test
    void testJoinSplit() {
        VertexId c1a = new VertexId("zone0:testJoinSplit-c1a");
        VertexId c1b = new VertexId("zone0:testJoinSplit-c1b");
        VertexId c1c = new VertexId("zone0:testJoinSplit-c1c");

        VertexId c2a = new VertexId("zone0:testJoin-c2a");
        VertexId c2b = new VertexId("zone0:testJoin-c2b");
        VertexId c2c = new VertexId("zone0:testJoin-c2c");

        Permissions p1 = new Permissions("10000");
        Permissions p2 = new Permissions("01000");
        Permissions p3 = new Permissions("00100");
        Permissions p4 = new Permissions("00010");
        Permissions p5 = new Permissions("00001");

        client.addVertex(c1a, Vertex.Type.GROUP);
        client.addVertex(c1b, Vertex.Type.GROUP);
        client.addVertex(c1c, Vertex.Type.GROUP);
        client.addVertex(c2a, Vertex.Type.GROUP);
        client.addVertex(c2b, Vertex.Type.GROUP);
        client.addVertex(c2c, Vertex.Type.GROUP);

        client.addEdge(zone, EdgeId.of(c1a, c1b), p1, "testJoinSplit-1");
        client.addEdge(zone, EdgeId.of(c1b, c1c), p2, "testJoinSplit-2");

        client.addEdge(zone, EdgeId.of(c2a, c2b), p3, "testJoinSplit-3");
        client.addEdge(zone, EdgeId.of(c2b, c2c), p4, "testJoinSplit-4");

        waitForIndex();

        assertThat(effectivePermissions(c1a, c2a)).isNull();
        assertThat(effectivePermissions(c1a, c2b)).isNull();
        assertThat(effectivePermissions(c1c, c2b)).isNull();
        assertThat(effectivePermissions(c1c, c2a)).isNull();

        assertThat(effectivePermissions(c1a, c1b)).isEqualTo(p1.toString());
        assertThat(effectivePermissions(c1b, c1c)).isEqualTo(p2.toString());

        assertThat(members(c2c))
                .containsExactlyInAnyOrder(c2a.toString(), c2b.toString());

        client.addEdge(zone, EdgeId.of(c1c, c2a), p5, "testJoinSplit-5");
        waitForIndex();

        assertThat(effectivePermissions(c1a, c2a)).isEqualTo(p5.toString());
        assertThat(effectivePermissions(c1a, c2b)).isEqualTo(p3.toString());
        assertThat(effectivePermissions(c1c, c2b)).isEqualTo(p3.toString());
        assertThat(effectivePermissions(c1c, c2a)).isEqualTo(p5.toString());
        assertThat(effectivePermissions(c1c, c2c)).isEqualTo(p4.toString());
        assertThat(effectivePermissions(c1a, c2c)).isEqualTo(p4.toString());

        assertThat(effectivePermissions(c1a, c1b)).isEqualTo(p1.toString());
        assertThat(effectivePermissions(c1b, c1c)).isEqualTo(p2.toString());

        assertThat(members(c2c))
                .containsExactlyInAnyOrder(
                        c1a.toString(), c1b.toString(), c1c.toString(),
                        c2a.toString(), c2b.toString());

        client.setPermissions(zone, EdgeId.of(c1c, c2a), p1, "testJoinSplit-6");
        waitForIndex();

        assertThat(effectivePermissions(c1a, c2a)).isEqualTo(p1.toString());
        assertThat(effectivePermissions(c1a, c2b)).isEqualTo(p3.toString());
        assertThat(effectivePermissions(c1c, c2b)).isEqualTo(p3.toString());
        assertThat(effectivePermissions(c1c, c2a)).isEqualTo(p1.toString());
        assertThat(effectivePermissions(c1c, c2c)).isEqualTo(p4.toString());
        assertThat(effectivePermissions(c1a, c2c)).isEqualTo(p4.toString());

        client.removeEdge(zone, EdgeId.of(c1c, c2a), "testJoinSplit-7");
        waitForIndex();

        assertThat(effectivePermissions(c1a, c2a)).isNull();
        assertThat(effectivePermissions(c1a, c2b)).isNull();
        assertThat(effectivePermissions(c1c, c2b)).isNull();
        assertThat(effectivePermissions(c1c, c2a)).isNull();

        assertThat(effectivePermissions(c1a, c1b)).isEqualTo(p1.toString());
        assertThat(effectivePermissions(c1b, c1c)).isEqualTo(p2.toString());

        assertThat(members(c2c))
                .containsExactlyInAnyOrder(c2a.toString(), c2b.toString());
    }
}
