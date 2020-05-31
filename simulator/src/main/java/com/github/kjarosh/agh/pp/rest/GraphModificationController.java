package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.index.EffectiveVertex;
import com.github.kjarosh.agh.pp.index.Inbox;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.index.events.EventType;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.kjarosh.agh.pp.Config.ZONE_ID;

/**
 * @author Kamil Jarosz
 */
@Controller
public class GraphModificationController {
    private static final Logger logger = LoggerFactory.getLogger(GraphModificationController.class);

    @Autowired
    private GraphLoader graphLoader;

    @Autowired
    private Inbox inbox;

    @RequestMapping(method = RequestMethod.POST, path = "graph/edges")
    @ResponseBody
    public void addEdge(
            @RequestParam("from") String fromIdString,
            @RequestParam("to") String toIdString,
            @RequestParam("permissions") String permissionsString,
            @RequestParam("successive") boolean successive) {
        Graph graph = graphLoader.getGraph();
        VertexId fromId = new VertexId(fromIdString);
        VertexId toId = new VertexId(toIdString);
        ZoneId fromOwner = fromId.owner();
        ZoneId toOwner = toId.owner();
        Permissions permissions = Strings.isNullOrEmpty(permissionsString) ? null :
                new Permissions(permissionsString);

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
                new ZoneClient().addEdge(fromOwner, fromId, toId, permissions);
                return;
            }

            new ZoneClient().addEdge(toOwner, fromId, toId, permissions, true);
        }

        graph.addEdge(new Edge(fromId, toId, permissions));
        if (successive) {
            Map<VertexId, EffectiveVertex> subjects = graph.getVertex(toId)
                    .index()
                    .getEffectiveParents()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));
            inbox.post(fromId, Event.builder()
                    .type(EventType.PARENT_CHANGE)
                    .effectiveVertices(subjects)
                    .sender(toId)
                    .build());
        } else {
            Map<VertexId, EffectiveVertex> subjects = graph.getVertex(fromId)
                    .index()
                    .getEffectiveChildren()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));
            inbox.post(toId, Event.builder()
                    .type(EventType.CHILD_CHANGE)
                    .effectiveVertices(subjects)
                    .sender(fromId)
                    .build());
        }
    }

    @RequestMapping(method = RequestMethod.POST, path = "graph/vertices")
    @ResponseBody
    public void addVertex(
            @RequestParam("name") String name,
            @RequestParam("type") Vertex.Type type) {
        Graph graph = graphLoader.getGraph();
        VertexId id = new VertexId(ZONE_ID, name);
        graph.addVertex(new Vertex(id, type));
    }
}
