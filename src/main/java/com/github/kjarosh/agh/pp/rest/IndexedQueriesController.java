package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.index.EffectiveVertex;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.rest.dto.EffectivePermissionsResponseDto;
import com.github.kjarosh.agh.pp.rest.dto.MembersResponseDto;
import com.github.kjarosh.agh.pp.rest.dto.ReachesResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.kjarosh.agh.pp.config.Config.ZONE_ID;

/**
 * @author Kamil Jarosz
 */
@Controller
public class IndexedQueriesController {
    @Autowired
    private GraphLoader graphLoader;

    @RequestMapping(method = RequestMethod.POST, path = "indexed/reaches")
    @ResponseBody
    public ReachesResponseDto reaches(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId) {
        EffectivePermissionsResponseDto eperms = effectivePermissions(fromId, toId);
        boolean reaches = eperms.getEffectivePermissions() != null &&
                !eperms.getEffectivePermissions().isEmpty();
        return ReachesResponseDto.builder()
                .reaches(reaches)
                .duration(eperms.getDuration())
                .build();
    }

    @RequestMapping(method = RequestMethod.POST, path = "indexed/members")
    @ResponseBody
    public MembersResponseDto members(
            @RequestParam("of") String ofId) {
        long start = System.nanoTime();
        Graph graph = graphLoader.getGraph();
        VertexId of = new VertexId(ofId);
        ZoneId ofOwner = of.owner();

        if (!ofOwner.equals(ZONE_ID)) {
            return new ZoneClient().naive().members(ofOwner, of);
        }

        Vertex ofVertex = graph.getVertex(of);
        List<String> members = ofVertex.index().getEffectiveChildrenSet()
                .stream()
                .map(VertexId::toString)
                .distinct()
                .collect(Collectors.toList());
        long end = System.nanoTime();
        return MembersResponseDto.builder()
                .members(members)
                .duration(Duration.ofNanos(end - start))
                .build();
    }

    @RequestMapping(method = RequestMethod.POST, path = "indexed/effective_permissions")
    @ResponseBody
    public EffectivePermissionsResponseDto effectivePermissions(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId) {
        long start = System.nanoTime();
        Graph graph = graphLoader.getGraph();
        EdgeId edgeId = EdgeId.of(
                new VertexId(fromId),
                new VertexId(toId));
        ZoneId toOwner = edgeId.getTo().owner();

        if (!toOwner.equals(ZONE_ID)) {
            return new ZoneClient().indexed().effectivePermissions(toOwner, edgeId);
        }

        Vertex toVertex = graph.getVertex(edgeId.getTo());
        String ep = toVertex.index()
                .getEffectiveChild(edgeId.getFrom())
                .map(EffectiveVertex::getEffectivePermissions)
                .map(Permissions::toString)
                .orElse(null);
        long end = System.nanoTime();
        return EffectivePermissionsResponseDto.builder()
                .effectivePermissions(ep)
                .duration(Duration.ofNanos(end - start))
                .build();
    }
}
