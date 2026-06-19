package com.fromzerotohero.mission.saas;

public enum Plan {
    FREE(25, 10),
    PRO(1_000, 500),
    BUSINESS(10_000, 5_000);

    private final int workItemLimit;
    private final int monthlyAgentRunLimit;

    Plan(int workItemLimit, int monthlyAgentRunLimit) {
        this.workItemLimit = workItemLimit;
        this.monthlyAgentRunLimit = monthlyAgentRunLimit;
    }

    public int workItemLimit() { return workItemLimit; }
    public int monthlyAgentRunLimit() { return monthlyAgentRunLimit; }
}
