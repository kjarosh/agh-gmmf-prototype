package com.github.kjarosh.agh.pp.index;

import com.github.kjarosh.agh.pp.graph.model.Edge;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.VertexId;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * @author Kamil Jarosz
 */
public interface EffectiveVertex {
    Permissions getEffectivePermissions();

    void setEffectivePermissions(Permissions calculated);

    Set<VertexId> getIntermediateVertices();

    void setIntermediateVertices(Set<VertexId> intermediateVertices);

    Set<VertexId> getIntermediateVerticesEager();

    boolean isDirty();

    void setDirty(boolean dirty);

    default void addIntermediateVertex(VertexId id, Runnable modifyListener) {
        addIntermediateVertices(Collections.singleton(id), modifyListener);
    }

    default void addIntermediateVertices(Set<VertexId> ids, Runnable modifyListener) {
        if (getIntermediateVertices().addAll(ids)) {
            modifyListener.run();
        }
    }

    default void removeIntermediateVertex(VertexId id, Runnable modifyListener) {
        removeIntermediateVertices(Collections.singleton(id), modifyListener);
    }

    default void removeIntermediateVertices(Set<VertexId> ids, Runnable modifyListener) {
        if (getIntermediateVertices().removeAll(ids)) {
            modifyListener.run();
        }
    }

    default CompletionStage<RecalculationResult> recalculatePermissions(Set<Edge> edgesToCalculate) {
        EffectiveVertex.CalculationResult result = calculatePermissions(edgesToCalculate);
        boolean wasDirty = getDirtyAndSetResult(result);
        if (result.isDirty()) {
            return CompletableFuture.completedFuture(RecalculationResult.DIRTY);
        } else if (wasDirty) {
            return CompletableFuture.completedFuture(RecalculationResult.CLEANED);
        } else {
            return CompletableFuture.completedFuture(RecalculationResult.CLEAN);
        }
    }

    default boolean getDirtyAndSetResult(CalculationResult result) {
        boolean wasDirty = isDirty();
        setDirty(result.isDirty());
        setEffectivePermissions(result.getCalculated());
        return wasDirty;
    }

    default CalculationResult calculatePermissions(Set<Edge> edgesToCalculate) {
        Set<VertexId> intermediateVertices = getIntermediateVerticesEager();
        List<Permissions> perms = edgesToCalculate.stream()
                .filter(x -> intermediateVertices.contains(x.src()))
                .map(Edge::permissions)
                .collect(Collectors.toList());
        Permissions effectivePermissions = perms.stream()
                .reduce(Permissions.NONE, Permissions::combine);
        boolean dirty = perms.size() != intermediateVertices.size();
        return CalculationResult.builder()
                .calculated(effectivePermissions)
                .dirty(dirty)
                .build();
    }

    enum RecalculationResult {
        CLEAN,
        CLEANED,
        DIRTY,
    }

    @Getter
    @Builder
    class CalculationResult {
        Permissions calculated;
        boolean dirty;
    }
}
