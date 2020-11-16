package com.github.kjarosh.agh.pp.test;

import com.github.kjarosh.agh.pp.config.Config;
import com.github.kjarosh.agh.pp.config.ZoneConfig;
import com.github.kjarosh.agh.pp.rest.client.GraphQueryClient;
import com.github.kjarosh.agh.pp.test.util.ExampleTestBase;
import com.github.kjarosh.agh.pp.test.util.GraphQueryClientArgumentsProvider;
import com.github.kjarosh.agh.pp.test.util.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.File;
import java.util.HashMap;

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
                .containsExactly("zone1:admins");
        assertThat(client.listAdjacent(zone, vid("zone1:anne")))
                .containsExactly("zone0:ceric", "zone1:audit", "zone1:members");
        assertThat(client.listAdjacent(zone, vid("zone1:krakow")))
                .isEmpty();

        assertThat(client.listAdjacentReversed(zone, vid("zone1:anne")))
                .isEmpty();
        assertThat(client.listAdjacentReversed(zone, vid("zone0:paris")))
                .containsExactly("zone0:datahub", "zone0:eo_data", "zone1:eosc");
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
        assertThat(queryClient.reaches(zone, eid("zone0:bob", "zone0:datahub")))
                .isTrue();
        assertThat(queryClient.reaches(zone, eid("zone0:bob", "zone1:admins")))
                .isFalse();
        assertThat(queryClient.reaches(zone, eid("zone0:bob", "zone0:dhub_members")))
                .isTrue();
        assertThat(queryClient.reaches(zone, eid("zone0:luke", "zone1:krakow")))
                .isTrue();
        assertThat(queryClient.reaches(zone, eid("zone1:anne", "zone0:lisbon")))
                .isFalse();
        assertThat(queryClient.reaches(zone, eid("zone0:luke", "zone0:dhub_members")))
                .isTrue();
        assertThat(queryClient.reaches(zone, eid("zone0:jill", "zone1:krakow")))
                .isTrue();
    }

    @ParameterizedTest
    @ArgumentsSource(GraphQueryClientArgumentsProvider.class)
    void members(GraphQueryClient queryClient) {
        assertThat(queryClient.members(zone, vid("zone1:admins"))).containsExactlyInAnyOrder(
                "zone0:uber_admins",
                "zone0:luke");

        assertThat(queryClient.members(zone, vid("zone0:eo_data"))).containsExactlyInAnyOrder(
                "zone0:dhub_mngrs",
                "zone0:dhub_members",
                "zone0:uber_admins",
                "zone1:admins",
                "zone0:alice",
                "zone0:bob",
                "zone0:luke");

        assertThat(queryClient.members(zone, vid("zone0:ebi"))).containsExactlyInAnyOrder(
                "zone0:alice",
                "zone0:jill");

        assertThat(queryClient.members(zone, vid("zone1:cyfnet"))).containsExactlyInAnyOrder(
                "zone1:audit",
                "zone1:members",
                "zone1:tom",
                "zone1:anne",
                "zone1:admins",
                "zone0:uber_admins",
                "zone0:luke");

        assertThat(queryClient.members(zone, vid("zone1:krakow"))).containsExactlyInAnyOrder(
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
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:alice", "zone0:bob")))
                .isNull();
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:jill", "zone1:krakow")))
                .isEqualTo("00000");
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:jill", "zone0:paris")))
                .isNull();
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:alice", "zone0:ebi")))
                .isEqualTo("11000");
        assertThat(queryClient.effectivePermissions(zone, eid("zone1:audit", "zone1:cyfnet")))
                .isEqualTo("11001");
        assertThat(queryClient.effectivePermissions(zone, eid("zone1:audit", "zone1:eosc")))
                .isEqualTo("11001");
        assertThat(queryClient.effectivePermissions(zone, eid("zone1:tom", "zone1:primage")))
                .isEqualTo("11011");
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:luke", "zone1:eosc")))
                .isEqualTo("11011");
        assertThat(queryClient.effectivePermissions(zone, eid("zone1:anne", "zone1:cyfnet")))
                .isEqualTo("11001");
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:luke", "zone0:dhub_members")))
                .isEqualTo("11111");
        assertThat(queryClient.effectivePermissions(zone, eid("zone1:admins", "zone1:eosc")))
                .isEqualTo("11011");
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:ebi", "zone0:ceric")))
                .isEqualTo("10000");
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:bob", "zone0:datahub")))
                .isEqualTo("11111");
        assertThat(queryClient.effectivePermissions(zone, eid("zone1:tom", "zone1:eosc")))
                .isEqualTo("11001");
        assertThat(queryClient.effectivePermissions(zone, eid("zone0:alice", "zone0:ceric")))
                .isEqualTo("10000");
    }
}
