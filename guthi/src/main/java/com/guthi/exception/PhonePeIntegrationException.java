package com.guthi.exception;

public class PhonePeIntegrationException extends RuntimeException {

    public PhonePeIntegrationException(String message) {
        super(message);
    }

    public PhonePeIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
