package com.github.kjarosh.agh.pp.graph.modification;

public interface OperationIssuer {
    void issue();

    OperationIssuer withOperationPerformer(OperationPerformer performer);
}
