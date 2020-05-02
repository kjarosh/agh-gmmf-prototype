package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.util.StringList;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collection;
import java.util.List;

/**
 * @author Kamil Jarosz
 */
public class ZoneClient {
    private UriComponentsBuilder baseUri(ZoneId zone) {
        return UriComponentsBuilder.fromHttpUrl("http://" + zone.getId() + "/");
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

    public boolean isAdjacent(ZoneId zone, VertexId from, VertexId to) {
        String url = baseUri(zone)
                .path("is_adjacent")
                .queryParam("from", from)
                .queryParam("to", to)
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

    public String permissions(ZoneId zone, VertexId from, VertexId to) {
        String url = baseUri(zone)
                .path("permissions")
                .queryParam("from", from)
                .queryParam("to", to)
                .build()
                .toUriString();
        return execute(url, String.class);
    }

    public boolean naiveReaches(ZoneId zone, VertexId from, VertexId to) {
        String url = baseUri(zone)
                .path("naive/reaches")
                .queryParam("from", from)
                .queryParam("to", to)
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

    public String naiveEffectivePermissions(ZoneId zone, VertexId from, VertexId to) {
        String url = baseUri(zone)
                .path("naive/effective_permissions")
                .queryParam("from", from)
                .queryParam("to", to)
                .build()
                .toUriString();
        return execute(url, String.class);
    }

    public boolean indexedReaches(ZoneId zone, VertexId from, VertexId to) {
        String url = baseUri(zone)
                .path("indexed/reaches")
                .queryParam("from", from)
                .queryParam("to", to)
                .build()
                .toUriString();
        return execute(url, Boolean.class);
    }

    public String indexedEffectivePermissions(ZoneId zone, VertexId from, VertexId to) {
        String url = baseUri(zone)
                .path("indexed/effective_permissions")
                .queryParam("from", from)
                .queryParam("to", to)
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

    public void addEdge(ZoneId zone, VertexId from, VertexId to, Permissions permissions) {
        addEdge(zone, from, to, permissions, false);
    }

    public void addEdge(ZoneId zone, VertexId from, VertexId to, Permissions permissions, boolean successive) {
        String url = baseUri(zone)
                .path("graph/edges")
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("permissions", permissions)
                .queryParam("successive", successive)
                .build()
                .toUriString();
        execute(url);
    }

    public void addVertex(ZoneId zone, VertexId id, Vertex.Type type) {
        String url = baseUri(zone)
                .path("graph/vertices")
                .queryParam("id", id)
                .queryParam("type", type)
                .build()
                .toUriString();
        execute(url);
    }
}
