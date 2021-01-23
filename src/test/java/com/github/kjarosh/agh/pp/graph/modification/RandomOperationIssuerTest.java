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

    @Test
    void randomElement0() {
        Random rand = Mockito.mock(Random.class);
        Mockito.when(rand.nextInt(Mockito.anyInt())).thenReturn(0);

        RandomOperationIssuer roi = new RandomOperationIssuer(rand, Mockito.mock(Graph.class));

        assertThat(roi.randomElementCollection(Arrays.asList(1, 2, 3))).isEqualTo(1);
        assertThat(roi.randomElementList(Arrays.asList(1, 2, 3))).isEqualTo(1);
    }

    @Test
    void randomElement2() {
        Random rand = Mockito.mock(Random.class);
        Mockito.when(rand.nextInt(Mockito.anyInt())).thenReturn(2);

        RandomOperationIssuer roi = new RandomOperationIssuer(rand, Mockito.mock(Graph.class));

        assertThat(roi.randomElementCollection(Arrays.asList(1, 2, 3, 4, 5))).isEqualTo(3);
        assertThat(roi.randomElementList(Arrays.asList(1, 2, 3, 4, 5))).isEqualTo(3);
    }
}
