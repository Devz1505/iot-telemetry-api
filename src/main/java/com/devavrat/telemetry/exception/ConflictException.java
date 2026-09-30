package com.devavrat.telemetry.exception;

/** Mapped to HTTP 409 by {@link GlobalExceptionHandler}. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
