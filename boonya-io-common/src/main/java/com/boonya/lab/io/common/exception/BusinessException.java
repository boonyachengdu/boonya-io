package com.boonya.lab.io.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public BusinessException(String message) {
        super(message);
        this.code = "BUSINESS_ERROR";
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public BusinessException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = "BUSINESS_ERROR";
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
}
