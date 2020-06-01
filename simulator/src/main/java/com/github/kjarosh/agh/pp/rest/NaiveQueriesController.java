package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.kjarosh.agh.pp.Config.ZONE_ID;

/**
 * @author Kamil Jarosz
 */
@Controller
public class NaiveQueriesController {
    private static final Logger logger = LoggerFactory.getLogger(NaiveQueriesController.class);

    @Autowired
    private GraphLoader graphLoader;

    @Autowired
    private BasicQueriesController basicQueriesController;

    @RequestMapping(method = RequestMethod.POST, path = "naive/reaches")
    @ResponseBody
    public boolean reaches(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId) {
        Graph graph = graphLoader.getGraph();
        EdgeId edgeId = EdgeId.of(
                new VertexId(fromId),
                new VertexId(toId));
        ZoneId fromOwner = edgeId.getFrom().owner();

        if (!fromOwner.equals(ZONE_ID)) {
            return new ZoneClient().naiveReaches(fromOwner, edgeId);
        }

        if (basicQueriesController.isAdjacent(fromId, toId)) {
            return true;
        }

        return graph.getEdgesBySource(edgeId.getFrom())
                .stream()
                .anyMatch(e -> reaches(e.dst().toString(), toId));
    }

    @RequestMapping(method = RequestMethod.POST, path = "naive/members")
    @ResponseBody
    public List<String> members(
            @RequestParam("of") String ofId) {
        Graph graph = graphLoader.getGraph();
        VertexId of = new VertexId(ofId);
        ZoneId ofOwner = of.owner();

        if (!ofOwner.equals(ZONE_ID)) {
            return new ZoneClient().naiveMembers(ofOwner, of);
        }

        Set<String> result = new HashSet<>();

        for (Edge edge : graph.getEdgesByDestination(of)) {
            result.add(edge.src().toString());
            result.addAll(members(edge.src().toString()));
        }

        return new ArrayList<>(result);
    }

    @RequestMapping(method = RequestMethod.POST, path = "naive/effective_permissions")
    @ResponseBody
    public String effectivePermissions(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId) {
        Graph graph = graphLoader.getGraph();
        EdgeId edgeId = EdgeId.of(
                new VertexId(fromId),
                new VertexId(toId));
        ZoneId fromOwner = edgeId.getFrom().owner();

        if (!fromOwner.equals(ZONE_ID)) {
            return new ZoneClient().naiveEffectivePermissions(fromOwner, edgeId);
        }

        Permissions permissions = null;

        for (Edge edge : graph.getEdgesBySource(edgeId.getFrom())) {
            if (edge.dst().equals(edgeId.getTo())) {
                permissions = Permissions.combine(
                        permissions,
                        edge.permissions());
            } else {
                String other = effectivePermissions(edge.dst().toString(), toId);
                permissions = Permissions.combine(
                        permissions,
                        other != null ? new Permissions(other) : null);
            }
        }

        return permissions == null ? null : permissions.toString();
    }
}
