package com.github.kjarosh.agh.pp.test;

import com.github.kjarosh.agh.pp.config.Config;
import com.github.kjarosh.agh.pp.config.ZoneConfig;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.rest.client.GraphQueryClient;
import com.github.kjarosh.agh.pp.test.util.ExampleTestBase;
import com.github.kjarosh.agh.pp.test.util.GraphQueryClientArgumentsProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.HashMap;

import static com.github.kjarosh.agh.pp.test.Assert.assertEqual;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Kamil Jarosz
 */
public class ExampleModificationGraphTest extends ExampleTestBase {
    @Override
    protected void afterSetup() {
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

    @ParameterizedTest
    @ArgumentsSource(GraphQueryClientArgumentsProvider.class)
    void effectivePermissions(GraphQueryClient queryClient) {
        assertEqual(queryClient.effectivePermissions(zone, eid("zone1:tom", "zone1:primage")),
                "11001");
        assertEqual(queryClient.effectivePermissions(zone, eid("zone0:luke", "zone1:admins")),
                "10100");
        assertEqual(queryClient.effectivePermissions(zone, eid("zone0:luke", "zone1:eosc")),
                "10001");
        assertEqual(queryClient.effectivePermissions(zone, eid("zone0:alice", "zone0:ceric")),
                null);
        assertEqual(queryClient.effectivePermissions(zone, eid("zone0:alice", "zone0:ebi")),
                null);
        assertEqual(queryClient.effectivePermissions(zone, eid("zone0:alice", "zone1:krakow")),
                "00000");
    }

    @ParameterizedTest
    @ArgumentsSource(GraphQueryClientArgumentsProvider.class)
    void members(GraphQueryClient queryClient) {
        assertThat(queryClient.members(zone, vid("zone0:ebi")))
                .containsExactlyInAnyOrder("zone0:jill");

        assertThat(queryClient.members(zone, vid("zone0:ceric")))
                .containsExactlyInAnyOrder(
                        "zone0:ebi",
                        "zone1:anne",
                        "zone0:jill");

        assertThat(queryClient.members(zone, vid("zone1:krakow")))
                .containsExactlyInAnyOrder(
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
                        "zone0:bob");
    }
}
