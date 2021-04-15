package com.github.kjarosh.agh.pp.graph.modification;

import java.io.IOException;

public interface IOperationPerformer {
    void perform() throws IOException;
    void setOperationIssuer(OperationIssuer issuer);
}
