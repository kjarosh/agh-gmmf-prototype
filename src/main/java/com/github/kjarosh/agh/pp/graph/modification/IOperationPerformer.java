package com.github.kjarosh.agh.pp.graph.modification;

import java.io.IOException;

public interface IOperationPerformer {
    void perform() throws IOException;
    IOperationPerformer withOperationIssuer(OperationIssuer issuer);
}
