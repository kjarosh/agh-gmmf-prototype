package com.github.kjarosh.agh.pp.graph.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Kamil Jarosz
 */
class PermissionsTest {
    @Test
    void invalidString() {
        assertThatThrownBy(() -> new Permissions(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Permissions("0"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Permissions("011010"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Permissions("20109"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void combine() {
        assertThat(Permissions.combine(
                new Permissions("00000"),
                new Permissions("00000")))
                .isEqualTo(new Permissions("00000"));
        assertThat(Permissions.combine(
                new Permissions("10000"),
                new Permissions("00000")))
                .isEqualTo(new Permissions("10000"));
        assertThat(Permissions.combine(
                new Permissions("10010"),
                new Permissions("00101")))
                .isEqualTo(new Permissions("10111"));
        assertThat(Permissions.combine(
                new Permissions("10011"),
                new Permissions("00111")))
                .isEqualTo(new Permissions("10111"));
    }
}
