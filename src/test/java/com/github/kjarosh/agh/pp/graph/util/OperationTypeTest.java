package com.github.kjarosh.agh.pp.graph.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperationTypeTest {
    @Test
    void test_serialize() throws JsonProcessingException {
        for(OperationType type : OperationType.values()) {
            new ObjectMapper().writeValueAsString(type);
        }
    }

    @Test
    void test_json_deserialize() throws JsonProcessingException {
        var mapper = new ObjectMapper().reader().forType(OperationType.class);
        for (OperationType type : OperationType.values()) {
            String value = new ObjectMapper().writeValueAsString(type);
            OperationType result = mapper.readValue(value);
            assertEquals(type, result);
        }

        assertThrows(JsonParseException.class, () -> {
            mapper.readValue("definitelly not a correct string");
        });
    }
}