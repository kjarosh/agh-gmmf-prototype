package com.github.kjarosh.agh.pp.rest.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Kamil Jarosz
 */
@ResponseStatus(value = HttpStatus.OK)
public class OkException extends RuntimeException {
}
