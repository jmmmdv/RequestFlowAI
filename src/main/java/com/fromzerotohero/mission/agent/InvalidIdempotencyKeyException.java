package com.fromzerotohero.mission.agent;

public class InvalidIdempotencyKeyException extends RuntimeException {
    public InvalidIdempotencyKeyException() {
        super("Idempotency-Key must contain 8-80 letters, numbers, dots, underscores, colons, or hyphens");
    }
}
