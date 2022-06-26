package com.github.kjarosh.agh.pp.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Kamil Jarosz
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModelObjectMapper {
    public static ObjectMapper INSTANCE = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

}
