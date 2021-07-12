package com.github.kjarosh.agh.pp.rest.error;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * @author Kamil Jarosz
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ErrorHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(VertexNotFoundException.class)
    protected ResponseEntity<Object> handleVertexNotFound(VertexNotFoundException ex) {
        logger.error(ex.getMessage());
        return ResponseEntity.status(404)
                .body(ErrorDto.builder()
                        .errorType("vertex_not_found")
                        .subject(ex.getVertex().toString())
                        .message(ex.getMessage())
                        .build());
    }

    @ExceptionHandler(EdgeNotFoundException.class)
    protected ResponseEntity<Object> handleEdgeNotFound(EdgeNotFoundException ex) {
        logger.error(ex.getMessage());
        return ResponseEntity.status(404)
                .body(ErrorDto.builder()
                        .errorType("edge_not_found")
                        .subject(ex.getEdge().toString())
                        .message(ex.getMessage())
                        .build());
    }
}
