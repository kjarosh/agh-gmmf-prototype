package com.github.kjarosh.agh.pp.rest.client;

import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.dto.EffectivePermissionsResponseDto;
import com.github.kjarosh.agh.pp.rest.dto.MembersResponseDto;
import com.github.kjarosh.agh.pp.rest.dto.ReachesResponseDto;

/**
 * @author Kamil Jarosz
 */
public interface GraphQueryClient {
    ReachesResponseDto reaches(ZoneId zone, EdgeId edgeId);

    MembersResponseDto members(ZoneId zone, VertexId of);

    EffectivePermissionsResponseDto effectivePermissions(ZoneId zone, EdgeId edgeId);
}
