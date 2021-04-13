package com.github.kjarosh.agh.pp.graph.generator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kjarosh.agh.pp.graph.modification.IOperationPerformer;
import com.github.kjarosh.agh.pp.graph.modification.OperationIssuer;
import com.github.kjarosh.agh.pp.graph.util.Operation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Queue;

public class SequenceOperationIssuer implements IOperationPerformer {

    Queue<Operation> queue;
    private OperationIssuer issuer;

    public SequenceOperationIssuer(String filepath) throws IOException {
        _load(filepath);
    }

    @Override
    public synchronized void perform() {
        Operation next = queue.remove();

        switch (next.getType()) {
            case ADD_EDGE: {
                addEdge(next);
                break;
            }
            case REMOVE_EDGE: {
                removeEdge(next);
                break;
            }
            case SET_PERMISSIONS: {
                setPermissions(next);
                break;
            }
            default:
                throw new IllegalArgumentException();
        }
    }

    private void setPermissions(Operation operation) {
        issuer.setPermissions(operation.getZoneId(), operation.getEdgeId(), operation.getPermissions(), operation.getTrace());
    }

    private void addEdge(Operation operation) {
        issuer.addEdge(operation.getZoneId(), operation.getEdgeId(), operation.getPermissions(), operation.getTrace());
    }

    private void removeEdge(Operation operation) {
        issuer.removeEdge(operation.getZoneId(), operation.getEdgeId(), operation.getTrace());
    }

    @Override
    public void setOperationIssuer(OperationIssuer issuer) {
        this.issuer = issuer;
    }

    private void _load(String filepath) throws IOException {
        File file = new File(filepath);
        if(!file.exists() || !file.isFile()) {
            throw new FileNotFoundException();
        }

    queue = new ObjectMapper().readValue(file, new TypeReference<>() {});
    }
}