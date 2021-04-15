package com.github.kjarosh.agh.pp.graph.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.kjarosh.agh.pp.graph.modification.IOperationPerformer;
import com.github.kjarosh.agh.pp.graph.modification.OperationIssuer;
import com.github.kjarosh.agh.pp.graph.util.Operation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class SequenceOperationIssuer implements IOperationPerformer {
    private OperationIssuer issuer;
    private final BufferedReader fileReader;
    private static final ObjectReader objectReader = new ObjectMapper().readerFor(Operation.class);

    public SequenceOperationIssuer(String filepath) throws IOException {
        fileReader = new BufferedReader(_openFile(filepath));
    }

    private synchronized Operation next() throws IOException {
        var str = fileReader.readLine();
        return objectReader.readValue(str.trim());
    }

    @Override
    public synchronized void perform() throws IOException {
        Operation next = next();

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
    public IOperationPerformer withOperationIssuer(OperationIssuer issuer) {
        this.issuer = issuer;
        return this;
    }

    private InputStreamReader _openFile(String filepath) throws IOException {
        var path = Path.of(filepath);
        if(!Files.exists(path) || Files.isDirectory(path)) {
            throw new IllegalArgumentException(filepath + "doesn't point to correct file");
        }

        return new InputStreamReader(Files.newInputStream(path));
    }
}