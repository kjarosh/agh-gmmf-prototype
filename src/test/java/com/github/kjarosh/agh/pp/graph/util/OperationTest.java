package com.github.kjarosh.agh.pp.graph.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OperationTest {
    @Test
    void test_serialize() throws JsonProcessingException {
        Operation op = new Operation(OperationType.ADD_EDGE, Map.of("zoneid", "oh"));
        new ObjectMapper().writeValueAsString(op);
    }

    @Test
    void test_deserialize() throws JsonProcessingException {
        var mapper = new ObjectMapper().reader().forType(Operation.class);

        Map<String, Object> args = Map.of("zoneid", "oh");
        Operation op = new Operation(OperationType.ADD_EDGE,args);
        var str = new ObjectMapper().writeValueAsString(op);
        var result = (Operation) mapper.readValue(str);

        assertEquals(op.getType(), result.getType());
        assertEquals(op.getProperties(), result.getProperties());
    }
}