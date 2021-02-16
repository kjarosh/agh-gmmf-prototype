package com.github.kjarosh.agh.pp.graph.modification;

import com.github.kjarosh.agh.pp.graph.model.Graph;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Kamil Jarosz
 */
class RandomOperationIssuerTest {
    @Test
    void trace() {
        RandomOperationIssuer roi = new RandomOperationIssuer(Mockito.mock(Graph.class));

        assertThat(roi.trace())
                .startsWith("generated-00000-");
        assertThat(roi.trace())
                .startsWith("generated-00001-");
        assertThat(roi.trace())
                .startsWith("generated-00002-");
    }
}
