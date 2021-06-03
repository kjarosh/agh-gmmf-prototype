package com.github.kjarosh.agh.pp.graph.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.graph.modification.OperationIssuer;
import com.github.kjarosh.agh.pp.graph.modification.OperationPerformer;
import com.github.kjarosh.agh.pp.graph.util.Operation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.NoSuchElementException;

public class SequenceOperationIssuer implements OperationIssuer {
    private final ObjectMapper mapper = new ObjectMapper();
    private final BufferedReader reader;
    private OperationPerformer performer;

    public SequenceOperationIssuer(InputStream is) {
        this.reader = new BufferedReader(new InputStreamReader(is));
    }

    private Operation getNext() {
        try {
            String line = reader.readLine();
            if (line == null) {
                return null;
            }
            return mapper.readValue(line, Operation.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
