package com.boonya.lab.io.common.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends BusinessException {

    public ValidationException(String field, String message) {
        super(
            "VALIDATION_ERROR",
            String.format("Validation failed for field '%s': %s", field, message),
            HttpStatus.UNPROCESSABLE_ENTITY
        );
    }

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
