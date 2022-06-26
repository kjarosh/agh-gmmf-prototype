package com.github.kjarosh.agh.pp.test.example;

import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.client.GraphQueryClient;
import com.github.kjarosh.agh.pp.test.util.ExampleTestBase;
import com.github.kjarosh.agh.pp.test.util.GraphQueryClientArgumentsProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Kamil Jarosz
 */
public class ExampleGraphTest extends ExampleTestBase {
    @Test
    void adjacent() {
        assertThat(client.isAdjacent(zone, eid("zone0:bob", "zone0:datahub")))
                .isTrue();
        assertThat(client.isAdjacent(zone, eid("zone0:bob", "zone0:alice")))
                .isFalse();
    }

    @Test
    void listAdjacent() {
        assertThat(client.listAdjacent(zone, vid("zone0:uber_admins")))
                .containsExactlyInAnyOrder("zone1:admins");
        assertThat(client.listAdjacent(zone, vid("zone1:anne")))
                .containsExactlyInAnyOrder("zone0:ceric", "zone1:audit", "zone1:members");
        assertThat(client.listAdjacent(zone, vid("zone1:krakow")))
                .isEmpty();

        assertThat(client.listAdjacentReversed(zone, vid("zone1:anne")))
                .isEmpty();
        assertThat(client.listAdjacentReversed(zone, vid("zone0:paris")))
                .containsExactlyInAnyOrder("zone0:datahub", "zone0:eo_data", "zone1:eosc");
    }

    @Test
    void permissions() {
        assertThat(client.permissions(zone, eid("zone0:alice", "zone0:bob")))
                .isNull();
        assertThat(client.permissions(zone, eid("zone0:alice", "zone0:ebi")))
                .isEqualTo("11000");
        assertThat(client.permissions(zone, eid("zone1:audit", "zone1:cyfnet")))
                .isEqualTo("11001");
        assertThat(client.permissions(zone, eid("zone1:audit", "zone1:eosc")))
                .isNull();
    }

    @ParameterizedTest
    @ArgumentsSource(GraphQueryClientArgumentsProvider.class)
    void reaches(GraphQueryClient queryClient) {
        assertThat(queryClient.reaches(zone, eid("zone0:bob", "zone0:datahub")).isReaches())
                .isTrue();
        assertThat(queryClient.reaches(zone, eid("zone0:bob", "zone1:admins")).isReaches())
                .isFalse();
        assertThat(queryClient.reaches(zone, eid("zone0:bob", "zone0:dhub_members")).isReaches())
                .isTrue();
        assertThat(queryClient.reaches(zone, eid("zone0:luke", "zone1:krakow")).isReaches())
                .isTrue();
        assertThat(queryClient.reaches(zone, eid("zone1:anne", "zone0:lisbon")).isReaches())
                .isFalse();
        assertThat(queryClient.reaches(zone, eid("zone0:luke", "zone0:dhub_members")).isReaches())
                .isTrue();
        assertThat(queryClient.reaches(zone, eid("zone0:jill", "zone1:krakow")).isReaches())
                .isTrue();
    }

    @ParameterizedTest
    @ArgumentsSource(GraphQueryClientArgumentsProvider.class)
    void members(GraphQueryClient queryClient) {
        assertThat(queryClient.members(zone, vid("zone1:admins")).getMembers()).containsExactlyInAnyOrder(
                "zone0:uber_admins",
                "zone0:luke");

        assertThat(queryClient.members(zone, vid("zone0:eo_data")).getMembers()).containsExactlyInAnyOrder(
                "zone0:dhub_mngrs",
                "zone0:dhub_members",
                "zone0:uber_admins",
                "zone1:admins",
                "zone0:alice",
                "zone0:bob",
                "zone0:luke");

        assertThat(queryClient.members(zone, vid("zone0:ebi")).getMembers()).containsExactlyInAnyOrder(
                "zone0:alice",
                "zone0:jill");

        assertThat(queryClient.members(zone, vid("zone1:cyfnet")).getMembers()).containsExactlyInAnyOrder(
                "zone1:audit",
                "zone1:members",
                "zone1:tom",
                "zone1:anne",
                "zone1:admins",
                "zone0:uber_admins",
                "zone0:luke");

        assertThat(queryClient.members(zone, vid("zone1:krakow")).getMembers()).containsExactlyInAnyOrder(
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

    @ParameterizedTest
    @ArgumentsSource(GraphQueryClientArgumentsProvider.class)
    void effectivePermissions(GraphQueryClient queryClient) {
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:alice", "zone0:bob")).getEffectivePermissions())
                .isNull();
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:jill", "zone1:krakow")).getEffectivePermissions())
                .isEqualTo("00000");
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:jill", "zone0:paris")).getEffectivePermissions())
                .isNull();
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:alice", "zone0:ebi")).getEffectivePermissions())
                .isEqualTo("11000");
        assertThat(queryClient.effectivePermissions(zone, eid("zone1:audit", "zone1:cyfnet")).getEffectivePermissions())
                .isEqualTo("11001");
        assertThat(queryClient.effectivePermissions(zone, eid("zone1:audit", "zone1:eosc")).getEffectivePermissions())
                .isEqualTo("11001");
        assertThat(queryClient.effectivePermissions(zone, eid("zone1:tom", "zone1:primage")).getEffectivePermissions())
                .isEqualTo("11011");
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:luke", "zone1:eosc")).getEffectivePermissions())
                .isEqualTo("11011");
        assertThat(queryClient.effectivePermissions(zone, eid("zone1:anne", "zone1:cyfnet")).getEffectivePermissions())
                .isEqualTo("11001");
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:luke", "zone0:dhub_members")).getEffectivePermissions())
                .isEqualTo("11111");
        assertThat(queryClient.effectivePermissions(zone, eid("zone1:admins", "zone1:eosc")).getEffectivePermissions())
                .isEqualTo("11011");
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:ebi", "zone0:ceric")).getEffectivePermissions())
                .isEqualTo("10000");
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:bob", "zone0:datahub")).getEffectivePermissions())
                .isEqualTo("11111");
        assertThat(queryClient.effectivePermissions(zone, eid("zone1:tom", "zone1:eosc")).getEffectivePermissions())
                .isEqualTo("11001");
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:alice", "zone0:ceric")).getEffectivePermissions())
                .isEqualTo("10000");
    }

    @Test
    void dependentZones() {
        assertThat(client.getDependentZones(zone).getZones())
                .containsExactlyInAnyOrder(new ZoneId("zone1"), new ZoneId("zone0"));
    }
}
