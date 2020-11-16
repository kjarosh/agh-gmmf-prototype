package com.github.kjarosh.agh.pp.test.util;

import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

/**
 * @author Kamil Jarosz
 */
public class GraphQueryClientArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        ZoneClient zc = new ZoneClient();
        return Stream.of(
                Arguments.of(zc.naive()),
                Arguments.of(zc.indexed())
        );
    }
}
