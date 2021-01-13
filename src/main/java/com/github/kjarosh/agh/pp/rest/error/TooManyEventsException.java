package com.github.kjarosh.agh.pp.rest.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Kamil Jarosz
 */
@ResponseStatus(value = HttpStatus.TOO_MANY_REQUESTS)
public class TooManyEventsException extends RuntimeException {
}
