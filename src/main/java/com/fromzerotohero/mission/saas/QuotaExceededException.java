package com.fromzerotohero.mission.saas;

public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(String resource, int limit) {
        super("The current plan allows " + limit + " " + resource + ". Upgrade the organization to continue.");
    }
}
