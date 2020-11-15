package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.config.Config;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.index.events.Event;
import com.github.kjarosh.agh.pp.index.events.EventStats;
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
public class ZoneClient {
    private UriComponentsBuilder baseUri(ZoneId zone) {
        return UriComponentsBuilder.fromHttpUrl("http://" + Config.getConfig().translateZoneToAddress(zone) + "/");
    }

    private <R> R execute(String url, Class<R> cls) {
        ResponseEntity<R> response = new RestTemplate().postForEntity(url, null, cls);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Status: " + response.getStatusCode());
        }

        return response.getBody();
    }

    private void execute(String url) {
        ResponseEntity<?> response = new RestTemplate().postForEntity(url, null, null);
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
            ResponseEntity<?> response = new RestTemplate().getForEntity(url, String.class);
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
        ResponseEntity<Boolean> response = new RestTemplate().getForEntity(url, Boolean.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Status: " + response.getStatusCode());
        }
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

    public boolean naiveReaches(ZoneId zone, EdgeId edgeId) {
        String url = baseUri(zone)
                .path("naive/reaches")
                .queryParam("from", edgeId.getFrom())
                .queryParam("to", edgeId.getTo())
                .build()
                .toUriString();
        return execute(url, Boolean.class);
    }

    public List<String> naiveMembers(ZoneId zone, VertexId of) {
        String url = baseUri(zone)
                .path("naive/members")
                .queryParam("of", of)
                .build()
                .toUriString();
        return execute(url, StringList.class);
    }

    public String naiveEffectivePermissions(ZoneId zone, EdgeId edgeId) {
        String url = baseUri(zone)
                .path("naive/effective_permissions")
                .queryParam("from", edgeId.getFrom())
                .queryParam("to", edgeId.getTo())
                .build()
                .toUriString();
        return execute(url, String.class);
    }

    public boolean indexedReaches(ZoneId zone, EdgeId edgeId) {
        String url = baseUri(zone)
                .path("indexed/reaches")
                .queryParam("from", edgeId.getFrom())
                .queryParam("to", edgeId.getTo())
                .build()
                .toUriString();
        return execute(url, Boolean.class);
    }

    public String indexedEffectivePermissions(ZoneId zone, EdgeId edgeId) {
        String url = baseUri(zone)
                .path("indexed/effective_permissions")
                .queryParam("from", edgeId.getFrom())
                .queryParam("to", edgeId.getTo())
                .build()
                .toUriString();
        return execute(url, String.class);
    }

    public Collection<String> indexedMembers(ZoneId zone, VertexId of) {
        String url = baseUri(zone)
                .path("indexed/members")
                .queryParam("of", of)
                .build()
                .toUriString();
        return execute(url, StringList.class);
    }

    public void addEdge(ZoneId zone, EdgeId edgeId, Permissions permissions) {
        addEdge(zone, edgeId, permissions, false);
    }

    public void addEdge(ZoneId zone, EdgeId edgeId, Permissions permissions, boolean successive) {
        String url = baseUri(zone)
                .path("graph/edges")
                .queryParam("from", edgeId.getFrom())
                .queryParam("to", edgeId.getTo())
                .queryParam("permissions", permissions)
                .queryParam("successive", successive)
                .build()
                .toUriString();
        execute(url);
    }

    public void removeEdge(ZoneId zone, EdgeId edgeId) {
        removeEdge(zone, edgeId, false);
    }

    public void removeEdge(ZoneId zone, EdgeId edgeId, boolean successive) {
        String url = baseUri(zone)
                .path("graph/edges/delete")
                .queryParam("from", edgeId.getFrom())
                .queryParam("to", edgeId.getTo())
                .queryParam("successive", successive)
                .build()
                .toUriString();
        execute(url);
    }

    public void setPermissions(ZoneId zone, EdgeId edgeId, Permissions permissions) {
        setPermissions(zone, edgeId, permissions, false);
    }

    public void setPermissions(ZoneId zone, EdgeId edgeId, Permissions permissions, boolean successive) {
        String url = baseUri(zone)
                .path("graph/edges/permissions")
                .queryParam("from", edgeId.getFrom())
                .queryParam("to", edgeId.getTo())
                .queryParam("permissions", permissions)
                .queryParam("successive", successive)
                .build()
                .toUriString();
        execute(url);
    }

    public void addVertex(VertexId id, Vertex.Type type) {
        String url = baseUri(id.owner())
                .path("graph/vertices")
                .queryParam("name", id.name())
                .queryParam("type", type)
                .build()
                .toUriString();
        execute(url);
    }

    public void postEvent(VertexId id, Event event) {
        String url = baseUri(id.owner())
                .path("events")
                .queryParam("id", id.toString())
                .build()
                .toUriString();
        ResponseEntity<?> response = new RestTemplate().postForEntity(url, event, null);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Status: " + response.getStatusCode());
        }
    }

    public EventStats getEventStats(ZoneId zone) {
        String url = baseUri(zone)
                .path("events/stats")
                .build()
                .toUriString();
        ResponseEntity<EventStats> response = new RestTemplate().getForEntity(url, EventStats.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Status: " + response.getStatusCode());
        }

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
        ResponseEntity<DependentZonesDto> response = new RestTemplate().postForEntity(url, exclude, DependentZonesDto.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Status: " + response.getStatusCode());
        }

        return response.getBody();
    }
}
