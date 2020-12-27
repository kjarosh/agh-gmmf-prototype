package com.github.kjarosh.agh.pp.rest.error;

import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import lombok.Getter;

/**
 * @author Kamil Jarosz
 */
public class EdgeNotFoundException extends RuntimeException {
    @Getter
    private final EdgeId edge;
    @Getter
    private final String actionDescription;

    public EdgeNotFoundException(EdgeId edge, String actionDescription) {
        super("Edge " + edge + " not found while " + actionDescription);
        this.edge = edge;
        this.actionDescription = actionDescription;
    }
}
