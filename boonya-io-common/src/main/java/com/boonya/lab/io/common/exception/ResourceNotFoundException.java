package com.boonya.lab.io.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resource, String id) {
        super(
            "RESOURCE_NOT_FOUND",
            String.format("%s not found with id: %s", resource, id),
            HttpStatus.NOT_FOUND
        );
    }

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
}
