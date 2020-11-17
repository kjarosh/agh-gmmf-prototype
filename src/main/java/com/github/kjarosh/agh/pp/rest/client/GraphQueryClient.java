package com.github.kjarosh.agh.pp.rest.client;

import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;

import java.util.List;

/**
 * @author Kamil Jarosz
 */
public interface GraphQueryClient {
    boolean reaches(ZoneId zone, EdgeId edgeId);

    List<String> members(ZoneId zone, VertexId of);

    String effectivePermissions(ZoneId zone, EdgeId edgeId);
}
