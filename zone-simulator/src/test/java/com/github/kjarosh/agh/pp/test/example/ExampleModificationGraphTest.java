package com.github.kjarosh.agh.pp.test.example;

import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.rest.client.GraphQueryClient;
import com.github.kjarosh.agh.pp.test.util.ExampleTestBase;
import com.github.kjarosh.agh.pp.test.util.GraphQueryClientArgumentsProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Kamil Jarosz
 */
public class ExampleModificationGraphTest extends ExampleTestBase {
    @Override
    protected void afterSetup() {
        client.setPermissions(zone,
                eid("zone1:tom", "zone1:primage"),
                new Permissions("11001"), null);
        client.setPermissions(zone,
                eid("zone0:uber_admins", "zone1:admins"),
                new Permissions("10100"), null);
        client.setPermissions(zone,
                eid("zone1:admins", "zone1:eosc"),
                new Permissions("10000"), null);
        client.setPermissions(zone,
                eid("zone1:cyfnet", "zone1:eosc"),
                new Permissions("00001"), null);

        client.removeEdge(zone,
                eid("zone0:alice", "zone0:ebi"), null);
        client.removeEdge(zone,
                eid("zone1:anne", "zone1:audit"), null);
    }

    @ParameterizedTest
    @ArgumentsSource(GraphQueryClientArgumentsProvider.class)
    void reaches(GraphQueryClient queryClient) {
        assertThat(queryClient.reaches(zone, eid("zone0:alice", "zone1:krakow")).isReaches())
                .isTrue();
    }

    @ParameterizedTest
    @ArgumentsSource(GraphQueryClientArgumentsProvider.class)
    void effectivePermissions(GraphQueryClient queryClient) {
        assertThat(queryClient.effectivePermissions(zone, eid("zone1:tom", "zone1:primage")).getEffectivePermissions())
                .isEqualTo("11001");
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:luke", "zone1:admins")).getEffectivePermissions())
                .isEqualTo("10100");
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:luke", "zone1:eosc")).getEffectivePermissions())
                .isEqualTo("10001");
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:alice", "zone0:ceric")).getEffectivePermissions())
                .isNull();
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:alice", "zone0:ebi")).getEffectivePermissions())
                .isNull();
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:alice", "zone1:krakow")).getEffectivePermissions())
                .isEqualTo("00000");
        assertThat(queryClient.effectivePermissions(zone, eid("zone1:anne", "zone1:cyfnet")).getEffectivePermissions())
                .isEqualTo("10000");
        assertThat(queryClient.effectivePermissions(zone, eid("zone1:anne", "zone1:audit")).getEffectivePermissions())
                .isNull();
        assertThat(queryClient.effectivePermissions(zone, eid("zone1:anne", "zone1:primage")).getEffectivePermissions())
                .isEqualTo("11000");
    }

    @ParameterizedTest
    @ArgumentsSource(GraphQueryClientArgumentsProvider.class)
    void members(GraphQueryClient queryClient) {
        assertThat(queryClient.members(zone, vid("zone0:ebi")).getMembers())
                .containsExactlyInAnyOrder("zone0:jill");

        assertThat(queryClient.members(zone, vid("zone0:ceric")).getMembers())
                .containsExactlyInAnyOrder(
                        "zone0:ebi",
                        "zone1:anne",
                        "zone0:jill");

        assertThat(queryClient.members(zone, vid("zone1:krakow")).getMembers())
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
