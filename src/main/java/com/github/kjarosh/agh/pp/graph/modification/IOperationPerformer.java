package com.github.kjarosh.agh.pp.graph.modification;

import com.github.kjarosh.agh.pp.graph.model.ZoneId;

import java.io.IOException;

public interface IOperationPerformer {
    void perform() throws IOException;
    IOperationPerformer withOperationIssuer(OperationIssuer issuer);
    void setZone(ZoneId zone);
}
