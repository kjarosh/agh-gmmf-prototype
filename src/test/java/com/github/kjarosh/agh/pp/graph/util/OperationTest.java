package com.github.kjarosh.agh.pp.graph.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationTest {
    @Test
    void test_serialize() throws JsonProcessingException {
        Operation op = new Operation(OperationType.ADD_EDGE, null, new ZoneId("d"), "trace", null);
        new ObjectMapper().writeValueAsString(op);
    }

    @Test
    void test_deserialize() throws JsonProcessingException {
        var mapper = new ObjectMapper().reader().forType(Operation.class);

        Operation op = new Operation(OperationType.ADD_EDGE, null, new ZoneId("oh"), null, null);
        var str = new ObjectMapper().writeValueAsString(op);
        var result = (Operation) mapper.readValue(str);

        assertEquals(op.getType(), result.getType());
        assertEquals(op.getZoneId(), result.getZoneId());
    }
}