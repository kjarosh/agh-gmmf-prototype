package com.github.kjarosh.agh.pp.graph.generator;

import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.graph.modification.OperationIssuer;
import com.github.kjarosh.agh.pp.graph.modification.OperationPerformer;
import com.github.kjarosh.agh.pp.graph.util.Operation;
import com.github.kjarosh.agh.pp.util.JsonLinesReader;

import java.io.InputStream;
import java.util.NoSuchElementException;

public class SequenceOperationIssuer implements OperationIssuer {
    private final JsonLinesReader reader;
    private OperationPerformer performer;

    public SequenceOperationIssuer(InputStream is) {
        this.reader = new JsonLinesReader(is);
    }

    private Operation getNext() {
        return reader.nextValue(Operation.class);
    }

    @Override
    public synchronized void issue() {
        Operation next = getNext();

        if (next == null) {
            throw new NoSuchElementException("No more operations");
        }

        switch (next.getType()) {
            case ADD_EDGE: {
                addEdge(next);
                break;
            }
            case REMOVE_EDGE: {
                removeEdge(next);
                break;
            }
            default:
                throw new IllegalArgumentException();
        }
    }

    private void addEdge(Operation op) {
        ZoneId zid = op.getZoneId();
        EdgeId eid = op.getEdgeId();
        Permissions permissions = op.getPermissions();
        String trace = op.getTrace();

        performer.addEdge(zid, eid, permissions, trace);
    }

    private void removeEdge(Operation op) {
        ZoneId zid = op.getZoneId();
        EdgeId eid = op.getEdgeId();
        String trace = op.getTrace();

        performer.removeEdge(zid, eid, trace);
    }

    @Override
    public SequenceOperationIssuer withOperationPerformer(OperationPerformer performer) {
        this.performer = performer;
        return this;
    }
}
