package com.github.kjarosh.agh.pp.rest;

import com.github.kjarosh.agh.pp.graph.GraphLoader;
import com.github.kjarosh.agh.pp.graph.model.Graph;
import com.github.kjarosh.agh.pp.graph.model.Vertex;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import com.github.kjarosh.agh.pp.index.EffectiveVertex;
import com.github.kjarosh.agh.pp.index.VertexIndex;
import com.github.kjarosh.agh.pp.rest.dto.IndexDto;
import com.github.kjarosh.agh.pp.rest.dto.IndexDto.EffectiveVertexDto;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Kamil Jarosz
 */
@Controller
public class GraphIndexController {
    private final GraphLoader graphLoader;

    public GraphIndexController(GraphLoader graphLoader) {
        this.graphLoader = graphLoader;
    }

    @RequestMapping(method = RequestMethod.GET, path = "index")
    @ResponseBody
    public List<IndexDto> getIndex(
            @RequestParam(value = "of", required = false) List<String> vertices) {
        Graph graph = graphLoader.getGraph();

        Stream<VertexIndex> indices;
        if (vertices == null || vertices.isEmpty()) {
            indices = graph.allVertices().stream()
                    .sorted()
                    .map(Vertex::index);
        } else {
            indices = vertices.stream()
                    .sorted()
                    .map(VertexId::new)
                    .map(graph::getVertex)
                    .map(Vertex::index);
        }

        return indices.map(this::mapIndex)
                .collect(Collectors.toList());
    }

    private IndexDto mapIndex(VertexIndex vertexIndex) {
        return IndexDto.builder()
                .children(mapEffectiveVertices(vertexIndex.getEffectiveChildren()))
                .parents(vertexIndex.getEffectiveParentsSet()
                        .stream()
                        .map(VertexId::toString)
                        .collect(Collectors.toSet()))
                .build();
    }

    private Map<String, EffectiveVertexDto> mapEffectiveVertices(Map<VertexId, EffectiveVertex> effectiveVertices) {
        Map<String, EffectiveVertexDto> ret = new HashMap<>();
        effectiveVertices.forEach((v, ev) -> {
            ret.put(v.toString(), EffectiveVertexDto.builder()
                    .permissions(ev.getEffectivePermissions().toString())
                    .intermediateVertices(ev.getIntermediateVertices().stream()
                            .map(VertexId::toString)
                            .collect(Collectors.toList()))
                    .build());
        });
        return ret;
    }
}
