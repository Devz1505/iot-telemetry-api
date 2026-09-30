package com.devavrat.telemetry.exception;

/**
 * A request that is well-formed JSON but breaks a business rule
 * (e.g. humidity of 140 %). Mapped to HTTP 400.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
