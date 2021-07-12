package com.github.kjarosh.agh.pp.graph.generator;

import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.graph.modification.OperationPerformer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;

/**
 * @author Kamil Jarosz
 */
class SequenceOperationIssuerTest {
    @Test
    void testIssue() {
        OperationPerformer performer = Mockito.mock(OperationPerformer.class);
        String json = "" +
                "{\"type\":\"ADD_EDGE\",\"zoneId\":\"z\",\"edgeId\":{\"from\":\"a:from\",\"to\":\"b:to\"},\"permissions\":\"10101\",\"trace\":\"t\"}\n" +
                "{\"type\":\"REMOVE_EDGE\",\"zoneId\":\"z2\",\"edgeId\":{\"from\":\"c:from\",\"to\":\"d:to\"},\"trace\":\"t2\"}\n";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());
        SequenceOperationIssuer i = new SequenceOperationIssuer(bais);
        i.withOperationPerformer(performer);

        i.issue();
        Mockito.verify(performer).addEdge(
                new ZoneId("z"),
                new EdgeId(new VertexId("a:from"), new VertexId("b:to")),
                new Permissions("10101"),
                "t");

        i.issue();
        Mockito.verify(performer).removeEdge(
                new ZoneId("z2"),
                new EdgeId(new VertexId("c:from"), new VertexId("d:to")),
                "t2");
    }
}
