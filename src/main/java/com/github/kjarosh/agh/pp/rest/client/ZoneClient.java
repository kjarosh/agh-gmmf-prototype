package com.github.kjarosh.agh.pp.rest.client;

import com.github.kjarosh.agh.pp.config.ConfigLoader;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.graph.modification.OperationIssuer;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.index.events.EventStats;
import com.github.kjarosh.agh.pp.rest.dto.BulkEdgeCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.BulkMessagesDto;
import com.github.kjarosh.agh.pp.rest.dto.BulkVertexCreationRequestDto;
import com.github.kjarosh.agh.pp.rest.dto.DependentZonesDto;
import com.github.kjarosh.agh.pp.util.StringList;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Kamil Jarosz
 */
public class ZoneClient implements OperationIssuer {
    private static final RestTemplate restTemplate = new RestTemplate();

    private final GraphQueryClient naiveGraphQueryClient;
    private final GraphQueryClient indexedGraphQueryClient;

    public ZoneClient() {
        this.naiveGraphQueryClient = new GraphQueryClientImpl("naive");
        this.indexedGraphQueryClient = new GraphQueryClientImpl("indexed");
    }

    private UriComponentsBuilder baseUri(ZoneId zone) {
        return UriComponentsBuilder.fromHttpUrl("http://" + ConfigLoader.getConfig().translateZoneToAddress(zone) + "/");
    }

    private <R> R execute(String url, Class<R> cls) {
        ResponseEntity<R> response = restTemplate.postForEntity(url, null, cls);
        checkResponse(response);

        return response.getBody();
    }

    private void execute(String url) {
        ResponseEntity<?> response = restTemplate.postForEntity(url, null, null);
        checkResponse(response);
    }

    private void checkResponse(ResponseEntity<?> response) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Status: " + response.getStatusCode());
        }
    }

    public boolean healthcheck(ZoneId zone) {
        try {
            String url = baseUri(zone)
                    .path("healthcheck")
                    .build()
                    .toUriString();
            ResponseEntity<?> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            return false;
        }
    }

    public boolean indexReady(ZoneId zone) {
        String url = baseUri(zone)
                .path("index_ready")
                .build()
                .toUriString();
        ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
        checkResponse(response);
        Boolean body = response.getBody();
        return body != null && body;
    }

    public boolean isAdjacent(ZoneId zone, EdgeId edgeId) {
        String url = baseUri(zone)
                .path("is_adjacent")
                .queryParam("from", edgeId.getFrom())
                .queryParam("to", edgeId.getTo())
                .build()
                .toUriString();
        return execute(url, Boolean.class);
    }

    public List<String> listAdjacent(ZoneId zone, VertexId of) {
        String url = baseUri(zone)
                .path("list_adjacent")
                .queryParam("of", of)
                .build()
                .toUriString();
        return execute(url, StringList.class);
    }

    public List<String> listAdjacentReversed(ZoneId zone, VertexId of) {
        String url = baseUri(zone)
                .path("list_adjacent_reversed")
                .queryParam("of", of)
                .build()
                .toUriString();
        return execute(url, StringList.class);
    }

    public String permissions(ZoneId zone, EdgeId edgeId) {
        String url = baseUri(zone)
                .path("permissions")
                .queryParam("from", edgeId.getFrom())
                .queryParam("to", edgeId.getTo())
                .build()
                .toUriString();
        return execute(url, String.class);
    }

    public GraphQueryClient naive() {
        return naiveGraphQueryClient;
    }

    public GraphQueryClient indexed() {
        return indexedGraphQueryClient;
    }

    @Override
    public void addEdge(ZoneId zone, EdgeId edgeId, Permissions permissions, String trace) {
        addEdge(zone, edgeId, permissions, trace, false);
    }

    @Override
    public void addEdges(ZoneId zone, BulkEdgeCreationRequestDto bulkRequest) {
        String url = baseUri(zone)
                .path("graph/edges/bulk")
                .build()
                .toUriString();
        ResponseEntity<?> response = restTemplate.postForEntity(url, bulkRequest, null);
        checkResponse(response);
    }

    public void addEdge(ZoneId zone, EdgeId edgeId, Permissions permissions, String trace, boolean successive) {
        UriComponentsBuilder builder = baseUri(zone)
                .path("graph/edges")
                .queryParam("from", edgeId.getFrom())
                .queryParam("to", edgeId.getTo())
                .queryParam("permissions", permissions)
                .queryParam("successive", successive);
        if (trace != null) {
            builder.queryParam("trace", trace);
        }
        String url = builder.build().toUriString();
        execute(url);
    }

    @Override
    public void removeEdge(ZoneId zone, EdgeId edgeId, String trace) {
        removeEdge(zone, edgeId, trace, false);
    }

    public void removeEdge(ZoneId zone, EdgeId edgeId, String trace, boolean successive) {
        UriComponentsBuilder builder = baseUri(zone)
                .path("graph/edges/delete")
                .queryParam("from", edgeId.getFrom())
                .queryParam("to", edgeId.getTo())
                .queryParam("successive", successive);
        if (trace != null) {
            builder.queryParam("trace", trace);
        }
        String url = builder.build().toUriString();
        execute(url);
    }

    @Override
    public void setPermissions(ZoneId zone, EdgeId edgeId, Permissions permissions, String trace) {
        setPermissions(zone, edgeId, permissions, trace, false);
    }

    public void setPermissions(ZoneId zone, EdgeId edgeId, Permissions permissions, String trace, boolean successive) {
        UriComponentsBuilder builder = baseUri(zone)
                .path("graph/edges/permissions")
                .queryParam("from", edgeId.getFrom())
                .queryParam("to", edgeId.getTo())
                .queryParam("permissions", permissions)
                .queryParam("successive", successive);
        if (trace != null) {
            builder.queryParam("trace", trace);
        }
        String url = builder.build().toUriString();
        execute(url);
    }

    @Override
    public void addVertex(VertexId id, Vertex.Type type) {
        String url = baseUri(id.owner())
                .path("graph/vertices")
                .queryParam("name", id.name())
                .queryParam("type", type)
                .build()
                .toUriString();
        execute(url);
    }

    @Override
    public void addVertices(ZoneId zone, BulkVertexCreationRequestDto bulkRequest) {
        String url = baseUri(zone)
                .path("graph/vertices/bulk")
                .build()
                .toUriString();
        ResponseEntity<?> response = restTemplate.postForEntity(url, bulkRequest, null);
        checkResponse(response);
    }

    public void postEvent(VertexId id, Event event) {
        String url = baseUri(id.owner())
                .path("events")
                .queryParam("id", id.toString())
                .build()
                .toUriString();
        ResponseEntity<?> response = restTemplate.postForEntity(url, event, null);
        checkResponse(response);
    }

    public void postEvents(ZoneId zone, BulkMessagesDto messages) {
        String url = baseUri(zone)
                .path("events/bulk")
                .build()
                .toUriString();
        ResponseEntity<?> response = restTemplate.postForEntity(url, messages, null);
        checkResponse(response);
    }

    public EventStats getEventStats(ZoneId zone) {
        String url = baseUri(zone)
                .path("events/stats")
                .build()
                .toUriString();
        ResponseEntity<EventStats> response = restTemplate.getForEntity(url, EventStats.class);
        checkResponse(response);

        return response.getBody();
    }

    public DependentZonesDto getDependentZones(ZoneId zone) {
        return getDependentZones(zone, Collections.emptyList());
    }

    public DependentZonesDto getDependentZones(ZoneId zone, Collection<ZoneId> exclude) {
        String url = baseUri(zone)
                .path("dependent_zones")
                .build()
                .toUriString();
        ResponseEntity<DependentZonesDto> response = restTemplate.postForEntity(url, exclude, DependentZonesDto.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Status: " + response.getStatusCode());
        }

        return response.getBody();
    }

    public boolean isInstrumentationEnabled(ZoneId zone) {
        String url = baseUri(zone)
                .path("instrumentation")
                .build()
                .toUriString();
        return restTemplate.getForObject(url, Boolean.class);
    }

    public void setInstrumentationEnabled(ZoneId zone, boolean enabled) {
        String url = baseUri(zone)
                .path("instrumentation")
                .build()
                .toUriString();
        restTemplate.put(url, enabled);
    }

    private class GraphQueryClientImpl implements GraphQueryClient {
        private final String prefix;

        public GraphQueryClientImpl(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public boolean reaches(ZoneId zone, EdgeId edgeId) {
            String url = baseUri(zone)
                    .path(prefix)
                    .path("/reaches")
                    .queryParam("from", edgeId.getFrom())
                    .queryParam("to", edgeId.getTo())
                    .build()
                    .toUriString();
            return execute(url, Boolean.class);
        }

        @Override
        public List<String> members(ZoneId zone, VertexId of) {
            String url = baseUri(zone)
                    .path(prefix)
                    .path("/members")
                    .queryParam("of", of)
                    .build()
                    .toUriString();
            return execute(url, StringList.class);
        }

        @Override
        public String effectivePermissions(ZoneId zone, EdgeId edgeId) {
            String url = baseUri(zone)
                    .path(prefix)
                    .path("/effective_permissions")
                    .queryParam("from", edgeId.getFrom())
                    .queryParam("to", edgeId.getTo())
                    .build()
                    .toUriString();
            return execute(url, String.class);
        }

        @Override
        public String toString() {
            return "GraphQueryClient(" + prefix + ')';
        }
    }
}
