package com.github.kjarosh.agh.pp.util;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Kamil Jarosz
 */
class RandomUtilsTest {

    @Test
    void randomElement0() {
        Random rand = Mockito.mock(Random.class);
        Mockito.when(rand.nextInt(Mockito.anyInt())).thenReturn(0);

        assertThat(RandomUtils.randomElementCollection(rand, Arrays.asList(1, 2, 3))).isEqualTo(1);
        assertThat(RandomUtils.randomElementList(rand, Arrays.asList(1, 2, 3))).isEqualTo(1);
    }

    @Test
    void randomElement2() {
        Random rand = Mockito.mock(Random.class);
        Mockito.when(rand.nextInt(Mockito.anyInt())).thenReturn(2);

        assertThat(RandomUtils.randomElementCollection(rand, Arrays.asList(1, 2, 3, 4, 5))).isEqualTo(3);
        assertThat(RandomUtils.randomElementList(rand, Arrays.asList(1, 2, 3, 4, 5))).isEqualTo(3);
    }
}
