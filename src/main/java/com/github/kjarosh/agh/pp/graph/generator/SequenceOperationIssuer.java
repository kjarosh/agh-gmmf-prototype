package com.github.kjarosh.agh.pp.graph.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kjarosh.agh.pp.graph.model.EdgeId;
import com.github.kjarosh.agh.pp.graph.model.Permissions;
import com.github.kjarosh.agh.pp.graph.model.ZoneId;
import com.github.kjarosh.agh.pp.graph.modification.IOperationPerformer;
import com.github.kjarosh.agh.pp.graph.modification.OperationIssuer;
import com.github.kjarosh.agh.pp.graph.util.Operation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;
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
                addEdge(next.getProperties());
                break;
            }
            case REMOVE_EDGE: {
                removeEdge(next.getProperties());
                break;
            }
            default:
                throw new IllegalArgumentException();
        }
    }

    private void addEdge(Map<String, Object> properties) {
        ZoneId zid = (ZoneId) properties.get("ZoneId");
        EdgeId eid = (EdgeId) properties.get("EdgeId");
        Permissions permissions = (Permissions) properties.get("Permissions");
        String trace = (String) properties.get("Trace");

        issuer.addEdge(zid, eid, permissions, trace);
    }

    private void removeEdge(Map<String, Object> properties) {
        ZoneId zid = (ZoneId) properties.get("ZoneId");
        EdgeId eid = (EdgeId) properties.get("EdgeId");
        String trace = (String) properties.get("Trace");

        issuer.removeEdge(zid, eid, trace);
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

        queue = (LinkedList<Operation>) new ObjectMapper().readValue(file, LinkedList.class);
    }
}