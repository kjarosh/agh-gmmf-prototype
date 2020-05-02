package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.github.kjarosh.agh.pp.Config.ZONE_ID;

/**
 * @author Kamil Jarosz
 */
@Controller
public class GraphModificationController {
    private static final Logger logger = LoggerFactory.getLogger(GraphModificationController.class);

    @Autowired
    private GraphLoader graphLoader;

    @RequestMapping(method = RequestMethod.POST, path = "graph/edges")
    @ResponseBody
    public void addEdge(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId,
            @RequestParam("permissions") String permissionsString,
            @RequestParam("successive") boolean successive) {
        Graph graph = graphLoader.getGraph();
        VertexId from = new VertexId(fromId);
        VertexId to = new VertexId(toId);
        ZoneId fromOwner = graph.getVertexOwner(from);
        ZoneId toOwner = graph.getVertexOwner(to);
        Permissions permissions = Strings.isNullOrEmpty(permissionsString) ? null : new Permissions(permissionsString);

        if (fromOwner == null) {
            throw new RuntimeException("From vertex is unknown: " + fromId);
        }

        if (toOwner == null) {
            throw new RuntimeException("To vertex is unknown: " + toId);
        }

        if (successive) {
            // if successive, we are getting this request from the other zone
            // which is the owner of the source vertex, we should be the destination
            if (!toOwner.equals(ZONE_ID)) {
                throw new RuntimeException("Destination zone different than this zone");
            }
        } else {
            // if it's the wrong zone, forward the request
            if (!fromOwner.equals(ZONE_ID)) {
                new ZoneClient().addEdge(fromOwner, from, to, permissions, true);
                return;
            }
        }

        graph.addEdge(new Edge(from, to, permissions));
    }

    @RequestMapping(method = RequestMethod.POST, path = "graph/vertices")
    @ResponseBody
    public void addVertex(
            @RequestParam("id") String idString,
            @RequestParam("type") Vertex.Type type) {
        Graph graph = graphLoader.getGraph();
        VertexId id = new VertexId(idString);
        graph.addVertex(new Vertex(id, type, ZONE_ID));
    }
}
