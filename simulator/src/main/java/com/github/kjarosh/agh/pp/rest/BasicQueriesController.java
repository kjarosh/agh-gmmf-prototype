package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
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

import java.util.List;
import java.util.stream.Collectors;

import static com.github.kjarosh.agh.pp.Config.ZONE_ID;

/**
 * @author Kamil Jarosz
 */
@Controller
public class BasicQueriesController {
    private static final Logger logger = LoggerFactory.getLogger(BasicQueriesController.class);

    @Autowired
    private GraphLoader graphLoader;

    @RequestMapping(method = RequestMethod.POST, path = "is_adjacent")
    @ResponseBody
    public boolean isAdjacent(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId) {
        Graph graph = graphLoader.getGraph();
        VertexId from = new VertexId(fromId);
        VertexId to = new VertexId(toId);
        ZoneId fromOwner = from.owner();

        if (!fromOwner.equals(ZONE_ID)) {
            return new ZoneClient().isAdjacent(fromOwner, from, to);
        }

        return graph.getEdgesBySource(from)
                .stream()
                .map(Edge::dst)
                .anyMatch(to::equals);
    }

    @RequestMapping(method = RequestMethod.POST, path = "list_adjacent")
    @ResponseBody
    public List<String> listAdjacent(
            @RequestParam("of") String ofId) {
        Graph graph = graphLoader.getGraph();
        VertexId of = new VertexId(ofId);
        ZoneId ofOwner = of.owner();

        if (!ofOwner.equals(ZONE_ID)) {
            return new ZoneClient().listAdjacent(ofOwner, of);
        }

        return graph.getEdgesBySource(of)
                .stream()
                .map(Edge::dst)
                .map(VertexId::toString)
                .collect(Collectors.toList());
    }

    @RequestMapping(method = RequestMethod.POST, path = "list_adjacent_reversed")
    @ResponseBody
    public List<String> listAdjacentReversed(
            @RequestParam("of") String ofId) {
        Graph graph = graphLoader.getGraph();
        VertexId of = new VertexId(ofId);
        ZoneId ofOwner = of.owner();

        if (!ofOwner.equals(ZONE_ID)) {
            return new ZoneClient().listAdjacentReversed(ofOwner, of);
        }

        return graph.getEdgesByDestination(of)
                .stream()
                .map(Edge::src)
                .map(VertexId::toString)
                .collect(Collectors.toList());
    }

    @RequestMapping(method = RequestMethod.POST, path = "permissions")
    @ResponseBody
    public String permissions(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId) {
        Graph graph = graphLoader.getGraph();
        VertexId from = new VertexId(fromId);
        VertexId to = new VertexId(toId);
        ZoneId fromOwner = from.owner();

        if (!fromOwner.equals(ZONE_ID)) {
            return new ZoneClient().permissions(fromOwner, from, to);
        }

        return graph.getEdgesBySource(from)
                .stream()
                .filter(e -> e.dst().equals(to))
                .findAny()
                .map(Edge::permissions)
                .map(Permissions::toString)
                .orElse("");
    }
}
