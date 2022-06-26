package com.github.kjarosh.agh.pp.graph.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperationTest {
    @Test
    void testSerialize() throws JsonProcessingException {
        Operation op = new Operation(OperationType.ADD_EDGE);
        op.setEdgeId(new EdgeId(new VertexId("a:x"), new VertexId("b:y")));
        new ObjectMapper().writeValueAsString(op);
    }

    @Test
    void testDeserialize() throws JsonProcessingException {
        var mapper = new ObjectMapper().reader().forType(Operation.class);

        Operation op = new Operation(OperationType.ADD_EDGE);
        op.setEdgeId(new EdgeId(new VertexId("a:x"), new VertexId("b:y")));
        var str = new ObjectMapper().writeValueAsString(op);
        var result = (Operation) mapper.readValue(str);

        assertEquals(op.getType(), result.getType());
        assertEquals(op.getEdgeId(), result.getEdgeId());
        assertEquals(op.getPermissions(), result.getPermissions());
    }
}
