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
import com.github.kjarosh.agh.pp.instrumentation.Instrumentation;
import com.github.kjarosh.agh.pp.instrumentation.Notification;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
import com.github.kjarosh.agh.pp.rest.dto.BulkEdgeCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.BulkVertexCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.EdgeCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.VertexCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.error.EdgeNotFoundException;
import com.github.kjarosh.agh.pp.rest.error.OkException;
import com.github.kjarosh.agh.pp.rest.error.VertexNotFoundException;
import com.github.kjarosh.agh.pp.rest.utils.GraphOperationPropagator;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.github.kjarosh.agh.pp.config.Config.ZONE_ID;

/**
 * @author Kamil Jarosz
 */
@Slf4j
@Controller
public class GraphModificationController {
    private final GraphLoader graphLoader;

    private final Inbox inbox;

    private final Instrumentation instrumentation = Instrumentation.getInstance();

    public GraphModificationController(GraphLoader graphLoader, Inbox inbox) {
        this.graphLoader = graphLoader;
        this.inbox = inbox;
    }

    @RequestMapping(method = RequestMethod.POST, path = "graph/edges")
    @ResponseBody
    public void addEdge(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId,
            @RequestParam("permissions") String permissionsString,
            @RequestParam(value = "trace", required = false) String traceParam,
            @RequestParam("successive") boolean successive) {
        Instant start = Instant.now();
        String trace = getTrace(traceParam);
        Graph graph = graphLoader.getGraph();
        EdgeId edgeId = EdgeId.of(
                new VertexId(fromId),
                new VertexId(toId));
        Permissions permissions = Strings.isNullOrEmpty(permissionsString) ? null :
                new Permissions(permissionsString);
        GraphOperationPropagator propagator = (zone, s) -> new ZoneClient()
                .addEdge(zone, edgeId, permissions, trace, s);

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
            log.info("Adding edge {}", edge);
        }

        graph.addEdge(edge);
        postChangeEvent(start, successive, trace, edgeId, false, false);
    }

    @RequestMapping(method = RequestMethod.POST, path = "graph/edges/bulk")
    @ResponseBody
    public void addEdges(@RequestBody BulkEdgeCreationRequestDto bulkRequest) {
        Instant start = Instant.now();
        addTrace(bulkRequest);
        Graph graph = graphLoader.getGraph();

        if (!bulkRequest.isSuccessive()) {
            BulkEdgeCreationRequestDto successiveBulkRequest = BulkEdgeCreationRequestDto.builder()
                    .successive(true)
                    .sourceZone(bulkRequest.getSourceZone())
                    .destinationZone(bulkRequest.getDestinationZone())
                    .edges(bulkRequest.getEdges())
                    .build();
            new ZoneClient().addEdges(bulkRequest.getDestinationZone(), successiveBulkRequest);
        }

        if (!bulkRequest.isSuccessive() || !bulkRequest.getDestinationZone().equals(bulkRequest.getSourceZone())) {
            log.info("Bulk adding {} edges", bulkRequest.getEdges().size());
        }

        for (EdgeCreationRequestDto request : bulkRequest.getEdges()) {
            VertexId src = new VertexId(bulkRequest.getSourceZone(), request.getFromName());
            VertexId dst = new VertexId(bulkRequest.getDestinationZone(), request.getToName());
            Edge edge = new Edge(src, dst, new Permissions(request.getPermissions()));
            graph.addEdge(edge);
            postChangeEvent(start, bulkRequest.isSuccessive(), request.getTrace(), edge.id(), false, false);
        }
    }

    @RequestMapping(method = RequestMethod.POST, path = "graph/edges/permissions")
    @ResponseBody
    public void setPermissions(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId,
            @RequestParam("permissions") String permissionsString,
            @RequestParam(value = "trace", required = false) String traceParam,
            @RequestParam("successive") boolean successive) {
        Instant start = Instant.now();
        String trace = getTrace(traceParam);
        Graph graph = graphLoader.getGraph();
        EdgeId edgeId = EdgeId.of(
                new VertexId(fromId),
                new VertexId(toId));
        Permissions permissions = Strings.isNullOrEmpty(permissionsString) ? null :
                new Permissions(permissionsString);
        GraphOperationPropagator propagator = (zone, s) -> new ZoneClient()
                .setPermissions(zone, edgeId, permissions, trace, s);

        optionallyForwardRequest(successive, edgeId, propagator);

        if (!graph.hasEdge(edgeId)) {
            throw new EdgeNotFoundException(edgeId, "setting permissions");
        }

        makeSuccessiveRequest(successive, edgeId, propagator);

        if (shouldLogOperation(successive, edgeId)) {
            log.info("Setting permissions of {} to {}", edgeId, permissions);
        }

        graph.setPermissions(edgeId, permissions);
        postChangeEvent(start, successive, trace, edgeId, false, true);
    }

    @RequestMapping(method = RequestMethod.POST, path = "graph/edges/delete")
    @ResponseBody
    public void removeEdge(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId,
            @RequestParam(value = "trace", required = false) String traceParam,
            @RequestParam("successive") boolean successive) {
        Instant start = Instant.now();
        String trace = getTrace(traceParam);
        Graph graph = graphLoader.getGraph();
        EdgeId edgeId = EdgeId.of(
                new VertexId(fromId),
                new VertexId(toId));
        GraphOperationPropagator propagator = (zone, s) ->
                new ZoneClient().removeEdge(zone, edgeId, trace, s);

        optionallyForwardRequest(successive, edgeId, propagator);

        Edge edge = graph.getEdge(edgeId);
        if (edge == null) {
            throw new EdgeNotFoundException(edgeId, "removing it");
        }

        makeSuccessiveRequest(successive, edgeId, propagator);

        if (shouldLogOperation(successive, edgeId)) {
            log.info("Removing edge {}", edge);
        }

        graph.removeEdge(edge);
        postChangeEvent(start, successive, trace, edgeId, true, true);
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
                log.info("Propagating request to zone " + fromOwner);
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
            Instant start,
            boolean successive,
            String trace,
            EdgeId edgeId,
            boolean delete,
            boolean permissionsChangedOnly) {
        Objects.requireNonNull(trace);
        Graph graph = graphLoader.getGraph();
        Set<VertexId> subjects = new HashSet<>();
        if (successive) {
            subjects.addAll(graph.getVertex(edgeId.getTo())
                    .index()
                    .getEffectiveParentsSet());
            subjects.add(edgeId.getTo());
            Event event = Event.builder()
                    .trace(trace)
                    .type(delete ? EventType.PARENT_REMOVE : EventType.PARENT_CHANGE)
                    .effectiveVertices(subjects)
                    .sender(edgeId.getTo())
                    .originalSender(edgeId.getTo())
                    .build();

            if (permissionsChangedOnly) {
                // no need to propagate, but inform about it
                VertexId id = edgeId.getFrom();
                instrumentation.notify(Notification.queued(id, event));
                instrumentation.notify(Notification.startProcessing(id, event));
                instrumentation.notify(Notification.endProcessing(id, event));
                return;
            }

            inbox.post(edgeId.getFrom(), event, start);
        } else {
            subjects.addAll(graph.getVertex(edgeId.getFrom())
                    .index()
                    .getEffectiveChildrenSet());
            subjects.add(edgeId.getFrom());
            inbox.post(edgeId.getTo(), Event.builder()
                    .trace(trace)
                    .type(delete ? EventType.CHILD_REMOVE : EventType.CHILD_CHANGE)
                    .effectiveVertices(subjects)
                    .sender(edgeId.getFrom())
                    .originalSender(edgeId.getFrom())
                    .build(), start);
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
        log.info("Adding vertex {}", id);
        graph.addVertex(new Vertex(id, type));
    }

    @RequestMapping(method = RequestMethod.POST, path = "graph/vertices/bulk")
    @ResponseBody
    public void addVertices(@RequestBody BulkVertexCreationRequestDto bulkRequest) {
        Graph graph = graphLoader.getGraph();

        int count = bulkRequest.getVertices().size();
        log.info("Bulk adding {} vertices", count);

        for (VertexCreationRequestDto request : bulkRequest.getVertices()) {
            VertexId id = new VertexId(ZONE_ID, request.getName());
            log.trace("Adding vertex {}", id);
            graph.addVertex(new Vertex(id, request.getType()));
        }
    }

    private String getTrace(String traceParam) {
        return traceParam != null ? traceParam : UUID.randomUUID().toString();
    }

    private void addTrace(BulkEdgeCreationRequestDto bulkRequest) {
        bulkRequest.getEdges().forEach(e -> {
            if (e.getTrace() == null) {
                e.setTrace(UUID.randomUUID().toString());
            }
        });
    }
}
