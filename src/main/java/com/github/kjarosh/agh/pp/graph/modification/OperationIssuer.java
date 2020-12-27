package com.github.kjarosh.agh.pp.graph.modification;

import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.dto.BulkVertexCreationRequestDto;

/**
 * @author Kamil Jarosz
 */
public interface OperationIssuer {
    void addEdge(ZoneId zone, EdgeId id, Permissions permissions);

    void removeEdge(ZoneId zone, EdgeId id);

    void setPermissions(ZoneId zone, EdgeId id, Permissions permissions);

    void addVertex(VertexId id, Vertex.Type type);

    void addVertices(ZoneId zone, BulkVertexCreationRequestDto bulkRequest);
}
