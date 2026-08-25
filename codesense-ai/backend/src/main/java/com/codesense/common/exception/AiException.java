package com.codesense.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class AiException extends RuntimeException {
    public AiException(String message) {
        super(message);
    }
    public AiException(String message, Throwable cause) {
        super(message, cause);
    }
}
