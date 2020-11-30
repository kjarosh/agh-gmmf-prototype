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
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.rest.error.EdgeNotFoundException;
import com.github.kjarosh.agh.pp.rest.error.OkException;
import com.github.kjarosh.agh.pp.rest.error.VertexNotFoundException;
import com.github.kjarosh.agh.pp.rest.utils.GraphOperationPropagator;
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
import java.util.Set;
import java.util.UUID;

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
        GraphOperationPropagator propagator = (zone, s) -> new ZoneClient()
                .addEdge(zone, edgeId, permissions, s);

        optionallyForwardRequest(successive, edgeId, propagator);

        if (!successive) {
            if (!graph.hasVertex(edgeId.getFrom())) {
                throw new VertexNotFoundException(edgeId.getFrom(), "adding edge " + edgeId);
            }
        } else {
            if (!graph.hasVertex(edgeId.getTo())) {
                throw new VertexNotFoundException(edgeId.getTo(), "adding edge " + edgeId);
            }
        }

        makeSuccessiveRequest(successive, edgeId, propagator);

        Edge edge = new Edge(edgeId.getFrom(), edgeId.getTo(), permissions);
        if (shouldLogOperation(successive, edgeId)) {
            logger.info("Adding edge {}", edge);
        }

        graph.addEdge(edge);
        postChangeEvent(successive, trace, edgeId, false);
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
        GraphOperationPropagator propagator = (zone, s) -> new ZoneClient()
                .setPermissions(zone, edgeId, permissions, s);

        optionallyForwardRequest(successive, edgeId, propagator);

        if (!graph.hasEdge(edgeId)) {
            throw new EdgeNotFoundException(edgeId, "setting permissions");
        }

        makeSuccessiveRequest(successive, edgeId, propagator);

        if (shouldLogOperation(successive, edgeId)) {
            logger.info("Setting permissions of {} to {}", edgeId, permissions);
        }

        graph.setPermissions(edgeId, permissions);
        postChangeEvent(successive, trace, edgeId, false);
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
        GraphOperationPropagator propagator = (zone, s) ->
                new ZoneClient().removeEdge(zone, edgeId, s);

        optionallyForwardRequest(successive, edgeId, propagator);

        Edge edge = graph.getEdge(edgeId);
        if (edge == null) {
            throw new EdgeNotFoundException(edgeId, "removing it");
        }

        makeSuccessiveRequest(successive, edgeId, propagator);

        if (shouldLogOperation(successive, edgeId)) {
            logger.info("Removing edge {}", edge);
        }

        graph.removeEdge(edge);
        postChangeEvent(successive, trace, edgeId, true);
    }

    private void optionallyForwardRequest(
            boolean successive,
            EdgeId edgeId,
            GraphOperationPropagator propagator) {
        ZoneId fromOwner = edgeId.getFrom().owner();
        ZoneId toOwner = edgeId.getTo().owner();
        if (fromOwner == null) {
            throw new RuntimeException("From vertex is unknown: " + edgeId.getFrom());
        }

        if (toOwner == null) {
            throw new RuntimeException("To vertex is unknown: " + edgeId.getTo());
        }

        if (!successive) {
            // if it's the wrong zone, forward the request
            if (!fromOwner.equals(ZONE_ID)) {
                logger.info("Propagating request to zone " + fromOwner);
                propagator.propagate(fromOwner, false);
                throw new OkException();
            }
        }
    }

    private void makeSuccessiveRequest(
            boolean successive,
            EdgeId edgeId,
            GraphOperationPropagator propagator) {
        ZoneId fromOwner = edgeId.getFrom().owner();
        ZoneId toOwner = edgeId.getTo().owner();

        if (successive) {
            // if successive, we are getting this request from the other zone
            // which is the owner of the source vertex, we should be the destination
            if (!toOwner.equals(ZONE_ID)) {
                throw new RuntimeException(String.format(
                        "Destination zone different than this zone (this=%s, dest=%s)", ZONE_ID, toOwner));
            }
        } else {
            if (!fromOwner.equals(ZONE_ID)) {
                throw new RuntimeException(String.format(
                        "Source zone different than this zone (this=%s, src=%s)", ZONE_ID, fromOwner));
            }

            propagator.propagate(toOwner, true);
        }
    }

    private void postChangeEvent(
            boolean successive,
            String trace,
            EdgeId edgeId,
            boolean delete) {
        if (trace == null) {
            trace = UUID.randomUUID().toString();
        }

        Graph graph = graphLoader.getGraph();
        if (successive) {
            Set<VertexId> subjects = graph.getVertex(edgeId.getTo())
                    .index()
                    .getEffectiveParents()
                    .keySet();
            inbox.post(edgeId.getFrom(), Event.builder()
                    .trace(trace)
                    .type(delete ? EventType.PARENT_REMOVE : EventType.PARENT_CHANGE)
                    .effectiveVertices(delete ? Collections.emptySet() : subjects)
                    .sender(edgeId.getTo())
                    .originalSender(edgeId.getTo())
                    .build());
        } else {
            Set<VertexId> subjects = graph.getVertex(edgeId.getFrom())
                    .index()
                    .getEffectiveChildren()
                    .keySet();
            inbox.post(edgeId.getTo(), Event.builder()
                    .trace(trace)
                    .type(delete ? EventType.CHILD_REMOVE : EventType.CHILD_CHANGE)
                    .effectiveVertices(delete ? Collections.emptySet() : subjects)
                    .sender(edgeId.getFrom())
                    .originalSender(edgeId.getFrom())
                    .build());
        }
    }

    private boolean shouldLogOperation(boolean successive, EdgeId edgeId) {
        ZoneId fromOwner = edgeId.getFrom().owner();
        ZoneId toOwner = edgeId.getTo().owner();
        // prevent double logging on the same zone
        return !successive || !fromOwner.equals(toOwner);
    }

    @RequestMapping(method = RequestMethod.POST, path = "graph/vertices")
    @ResponseBody
    public void addVertex(
            @RequestParam("name") String name,
            @RequestParam("type") Vertex.Type type) {
        Graph graph = graphLoader.getGraph();
        VertexId id = new VertexId(ZONE_ID, name);
        logger.info("Adding vertex {}", id);
        graph.addVertex(new Vertex(id, type));
    }
}
