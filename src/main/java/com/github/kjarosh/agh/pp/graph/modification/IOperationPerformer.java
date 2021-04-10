package com.github.kjarosh.agh.pp.graph.modification;

public interface IOperationPerformer {
    void perform();
    void setOperationIssuer(OperationIssuer issuer);
}
