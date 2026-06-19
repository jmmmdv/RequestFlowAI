package com.fromzerotohero.mission.saas;

import org.springframework.http.HttpStatus;

public class SaasException extends RuntimeException {
    private final HttpStatus status;
    public SaasException(HttpStatus status, String message) { super(message); this.status = status; }
    public HttpStatus status() { return status; }
}
