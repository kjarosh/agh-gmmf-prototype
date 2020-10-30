package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.index.Inbox;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.index.events.EventType;
import com.github.kjarosh.agh.pp.rest.utils.OkException;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Set;
import java.util.function.BiConsumer;

import static com.github.kjarosh.agh.pp.config.Config.ZONE_ID;

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
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId,
            @RequestParam("permissions") String permissionsString,
            @RequestParam(value = "trace", required = false) String trace,
            @RequestParam("successive") boolean successive) {
        Graph graph = graphLoader.getGraph();
        EdgeId edgeId = EdgeId.of(
                new VertexId(fromId),
                new VertexId(toId));
        Permissions permissions = Strings.isNullOrEmpty(permissionsString) ? null :
                new Permissions(permissionsString);

        makeSuccessiveRequest(successive, edgeId,
                (zone, s) -> new ZoneClient()
                        .addEdge(zone, edgeId, permissions, s));

        Edge edge = new Edge(edgeId.getFrom(), edgeId.getTo(), permissions);
        logger.info("Adding edge {}", edge);
        graph.addEdge(edge);
        postChangeEvent(successive, trace, edgeId);
    }

    @RequestMapping(method = RequestMethod.POST, path = "graph/edges/permissions")
    @ResponseBody
    public void setPermissions(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId,
            @RequestParam("permissions") String permissionsString,
            @RequestParam(value = "trace", required = false) String trace,
            @RequestParam("successive") boolean successive) {
        Graph graph = graphLoader.getGraph();
        EdgeId edgeId = EdgeId.of(
                new VertexId(fromId),
                new VertexId(toId));
        Permissions permissions = Strings.isNullOrEmpty(permissionsString) ? null :
                new Permissions(permissionsString);

        makeSuccessiveRequest(successive, edgeId,
                (zone, s) -> new ZoneClient()
                        .setPermissions(zone, edgeId, permissions, s));

        logger.info("Setting permissions of {} to {}", edgeId, permissions);
        graph.setPermissions(edgeId, permissions);
        postChangeEvent(successive, trace, edgeId);
    }

    @RequestMapping(method = RequestMethod.POST, path = "graph/edges/delete")
    @ResponseBody
    public void removeEdge(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId,
            @RequestParam(value = "trace", required = false) String trace,
            @RequestParam("successive") boolean successive) {
        Graph graph = graphLoader.getGraph();
        EdgeId edgeId = EdgeId.of(
                new VertexId(fromId),
                new VertexId(toId));

        makeSuccessiveRequest(successive, edgeId,
                (zone, s) -> new ZoneClient()
                        .removeEdge(zone, edgeId, s));

        Edge edge = graph.getEdge(edgeId);
        logger.info("Removing edge {}", edge);
        graph.removeEdge(edge);
        postChangeEvent(successive, trace, edgeId);
    }

    private void makeSuccessiveRequest(
            boolean successive,
            EdgeId edgeId,
            BiConsumer<ZoneId, Boolean> propagator) {
        ZoneId fromOwner = edgeId.getFrom().owner();
        ZoneId toOwner = edgeId.getTo().owner();
        if (fromOwner == null) {
            throw new RuntimeException("From vertex is unknown: " + edgeId.getFrom());
        }

        if (toOwner == null) {
            throw new RuntimeException("To vertex is unknown: " + edgeId.getTo());
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
                propagator.accept(fromOwner, false);
                throw new OkException();
            }

            propagator.accept(toOwner, true);
        }
    }

    private void postChangeEvent(
            boolean successive,
            String trace,
            EdgeId edgeId) {
        Graph graph = graphLoader.getGraph();
        if (successive) {
            Set<VertexId> subjects = graph.getVertex(edgeId.getTo())
                    .index()
                    .getEffectiveParents();
            inbox.post(edgeId.getFrom(), Event.builder()
                    .trace(trace)
                    .type(EventType.PARENT_CHANGE)
                    .effectiveVertices(subjects)
                    .sender(edgeId.getTo())
                    .build());
        } else {
            Set<VertexId> subjects = graph.getVertex(edgeId.getFrom())
                    .index()
                    .getEffectiveChildren()
                    .keySet();
            inbox.post(edgeId.getTo(), Event.builder()
                    .trace(trace)
                    .type(EventType.CHILD_CHANGE)
                    .effectiveVertices(subjects)
                    .sender(edgeId.getFrom())
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
