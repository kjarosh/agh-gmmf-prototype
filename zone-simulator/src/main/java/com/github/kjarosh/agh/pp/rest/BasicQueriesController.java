package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.stream.Collectors;

import static com.github.kjarosh.agh.pp.config.Config.ZONE_ID;

/**
 * @author Kamil Jarosz
 */
@Controller
public class BasicQueriesController {
    private final GraphLoader graphLoader;

    public BasicQueriesController(GraphLoader graphLoader) {
        this.graphLoader = graphLoader;
    }

    @RequestMapping(method = RequestMethod.POST, path = "is_adjacent")
    @ResponseBody
    public boolean isAdjacent(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId) {
        Graph graph = graphLoader.getGraph();
        EdgeId edgeId = EdgeId.of(
                new VertexId(fromId),
                new VertexId(toId));
        ZoneId fromOwner = edgeId.getFrom().owner();

        if (!fromOwner.equals(ZONE_ID)) {
            return new ZoneClient().isAdjacent(fromOwner, edgeId);
        }

        return graph.hasEdge(edgeId);
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

        return graph.getDestinationsBySource(of)
                .stream()
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

        return graph.getSourcesByDestination(of)
                .stream()
                .map(VertexId::toString)
                .collect(Collectors.toList());
    }

    @RequestMapping(method = RequestMethod.POST, path = "permissions")
    @ResponseBody
    public String permissions(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId) {
        Graph graph = graphLoader.getGraph();
        EdgeId edgeId = EdgeId.of(
                new VertexId(fromId),
                new VertexId(toId));
        ZoneId fromOwner = edgeId.getFrom().owner();

        if (!fromOwner.equals(ZONE_ID)) {
            return new ZoneClient().permissions(fromOwner, edgeId);
        }

        Edge edge = graph.getEdge(edgeId);
        return edge != null ? edge.permissions().toString() : "";
    }
}
