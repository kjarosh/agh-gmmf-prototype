package com.github.kjarosh.agh.pp.graph.modification;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Kamil Jarosz
 */
class BulkOperationIssuerTest {
    @Test
    void toTimeSpan() {
        assertThat(BulkOperationIssuer.toTimeSpan(1, 10))
                .isEqualTo(Duration.ofMillis(100));
        assertThat(BulkOperationIssuer.toTimeSpan(1, 1))
                .isEqualTo(Duration.ofMillis(1000));
    }
}
