package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.index.EffectiveVertex;
import com.github.kjarosh.agh.pp.rest.client.ZoneClient;
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

import static com.github.kjarosh.agh.pp.config.Config.ZONE_ID;

/**
 * @author Kamil Jarosz
 */
@Controller
public class IndexedQueriesController {
    private static final Logger logger = LoggerFactory.getLogger(NaiveQueriesController.class);

    @Autowired
    private GraphLoader graphLoader;

    @RequestMapping(method = RequestMethod.POST, path = "indexed/reaches")
    @ResponseBody
    public boolean reaches(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId) {
        String eperms = effectivePermissions(fromId, toId);
        return eperms != null && !eperms.isEmpty();
    }

    @RequestMapping(method = RequestMethod.POST, path = "indexed/members")
    @ResponseBody
    public List<String> members(
            @RequestParam("of") String ofId) {
        Graph graph = graphLoader.getGraph();
        VertexId of = new VertexId(ofId);
        ZoneId ofOwner = of.owner();

        if (!ofOwner.equals(ZONE_ID)) {
            return new ZoneClient().naive().members(ofOwner, of);
        }

        Vertex ofVertex = graph.getVertex(of);
        return ofVertex.index().getEffectiveChildren()
                .keySet()
                .stream()
                .map(VertexId::toString)
                .collect(Collectors.toList());
    }

    @RequestMapping(method = RequestMethod.POST, path = "indexed/effective_permissions")
    @ResponseBody
    public String effectivePermissions(
            @RequestParam("from") String fromId,
            @RequestParam("to") String toId) {
        Graph graph = graphLoader.getGraph();
        EdgeId edgeId = EdgeId.of(
                new VertexId(fromId),
                new VertexId(toId));
        ZoneId toOwner = edgeId.getTo().owner();

        if (!toOwner.equals(ZONE_ID)) {
            return new ZoneClient().indexed().effectivePermissions(toOwner, edgeId);
        }

        Vertex toVertex = graph.getVertex(edgeId.getTo());
        EffectiveVertex effectiveVertex = toVertex.index()
                .getEffectiveChildren().get(edgeId.getFrom());

        if (effectiveVertex == null) {
            return null;
        }

        return effectiveVertex.getEffectivePermissions().toString();
    }
}
