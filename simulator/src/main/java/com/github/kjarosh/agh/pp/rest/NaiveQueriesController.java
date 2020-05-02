package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
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
        VertexId from = new VertexId(fromId);
        VertexId to = new VertexId(toId);
        ZoneId fromOwner = graph.getVertexOwner(from);

        if (!fromOwner.equals(ZONE_ID)) {
            return new ZoneClient().naiveReaches(fromOwner, from, to);
        }

        if (basicQueriesController.isAdjacent(fromId, toId)) {
            return true;
        }

        return graph.getEdgesBySource(from)
                .stream()
                .anyMatch(e -> reaches(e.dst().getId(), toId));
    }

    @RequestMapping(method = RequestMethod.POST, path = "naive/members")
    @ResponseBody
    public List<String> members(
            @RequestParam("of") String ofId) {
        Graph graph = graphLoader.getGraph();
        VertexId of = new VertexId(ofId);
        ZoneId ofOwner = graph.getVertexOwner(of);

        if (!ofOwner.equals(ZONE_ID)) {
            return new ZoneClient().naiveMembers(ofOwner, of);
        }

        Set<String> result = new HashSet<>();

        Vertex vertex = graph.getVertex(of);
        if (vertex.type() == Vertex.Type.USER) {
            result.add(of.getId());
        }

        for (Edge edge : graph.getEdgesByDestination(of)) {
            result.addAll(members(edge.src().getId()));
        }

        return new ArrayList<>(result);
    }

    @RequestMapping(method = RequestMethod.POST, path = "naive/effective_permissions")
    @ResponseBody
    public String effectivePermissions(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId) {
        Graph graph = graphLoader.getGraph();
        VertexId from = new VertexId(fromId);
        VertexId to = new VertexId(toId);
        ZoneId fromOwner = graph.getVertexOwner(from);

        if (!fromOwner.equals(ZONE_ID)) {
            return new ZoneClient().naiveEffectivePermissions(fromOwner, from, to);
        }

        Permissions permissions = null;

        for (Edge edge : graph.getEdgesBySource(from)) {
            if (edge.dst().equals(to)) {
                permissions = Permissions.combine(
                        permissions,
                        edge.permissions());
            } else {
                String other = effectivePermissions(edge.dst().getId(), toId);
                permissions = Permissions.combine(
                        permissions,
                        other != null ? new Permissions(other) : null);
            }
        }

        return permissions == null ? null : permissions.toString();
    }
}
