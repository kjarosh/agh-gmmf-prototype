package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.rest.dto.EffectivePermissionsResponseDto;
import com.github.kjarosh.agh.pp.rest.dto.MembersResponseDto;
import com.github.kjarosh.agh.pp.rest.dto.ReachesResponseDto;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static com.github.kjarosh.agh.pp.config.Config.ZONE_ID;

/**
 * @author Kamil Jarosz
 */
@Controller
public class NaiveQueriesController {
    private final GraphLoader graphLoader;

    private final BasicQueriesController basicQueriesController;

    public NaiveQueriesController(GraphLoader graphLoader, BasicQueriesController basicQueriesController) {
        this.graphLoader = graphLoader;
        this.basicQueriesController = basicQueriesController;
    }

    @RequestMapping(method = RequestMethod.POST, path = "naive/reaches")
    @ResponseBody
    public ReachesResponseDto reaches(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId) {
        long start = System.nanoTime();
        Graph graph = graphLoader.getGraph();
        EdgeId edgeId = EdgeId.of(
                new VertexId(fromId),
                new VertexId(toId));
        ZoneId fromOwner = edgeId.getFrom().owner();

        if (!fromOwner.equals(ZONE_ID)) {
            return new ZoneClient().naive().reaches(fromOwner, edgeId);
        }

        if (basicQueriesController.isAdjacent(fromId, toId)) {
            long end = System.nanoTime();
            return ReachesResponseDto.builder()
                    .reaches(true)
                    .duration(Duration.ofNanos(end - start))
                    .build();
        }

        boolean reaches = graph.getEdgesBySource(edgeId.getFrom())
                .stream()
                .anyMatch(e -> {
                    try {
                        return reaches(e.dst().toString(), toId).isReaches();
                    } catch (StackOverflowError err) {
                        throw new RuntimeException("Found a cycle");
                    }
                });
        long end = System.nanoTime();
        return ReachesResponseDto.builder()
                .reaches(reaches)
                .duration(Duration.ofNanos(end - start))
                .build();
    }

    @RequestMapping(method = RequestMethod.POST, path = "naive/members")
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

        Set<String> result = new HashSet<>();

        for (Edge edge : graph.getEdgesByDestination(of)) {
            result.add(edge.src().toString());
            try {
                result.addAll(members(edge.src().toString()).getMembers());
            } catch (StackOverflowError e) {
                throw new RuntimeException("Found a cycle");
            }
        }

        long end = System.nanoTime();
        return MembersResponseDto.builder()
                .members(new ArrayList<>(result))
                .duration(Duration.ofNanos(end - start))
                .build();
    }

    @RequestMapping(method = RequestMethod.POST, path = "naive/effective_permissions")
    @ResponseBody
    public EffectivePermissionsResponseDto effectivePermissions(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId) {
        long start = System.nanoTime();
        Graph graph = graphLoader.getGraph();
        EdgeId edgeId = EdgeId.of(
                new VertexId(fromId),
                new VertexId(toId));
        ZoneId fromOwner = edgeId.getFrom().owner();

        if (!fromOwner.equals(ZONE_ID)) {
            return new ZoneClient().naive().effectivePermissions(fromOwner, edgeId);
        }

        Permissions permissions = null;

        for (Edge edge : graph.getEdgesBySource(edgeId.getFrom())) {
            if (edge.dst().equals(edgeId.getTo())) {
                permissions = Permissions.combine(
                        permissions,
                        edge.permissions());
            } else {
                try {
                    String other = effectivePermissions(edge.dst().toString(), toId).getEffectivePermissions();
                    permissions = Permissions.combine(
                            permissions,
                            other != null ? new Permissions(other) : null);
                } catch (StackOverflowError e) {
                    throw new RuntimeException("Found a cycle");
                }
            }
        }

        long end = System.nanoTime();
        return EffectivePermissionsResponseDto.builder()
                .effectivePermissions(permissions == null ? null : permissions.toString())
                .duration(Duration.ofNanos(end - start))
                .build();
    }
}
