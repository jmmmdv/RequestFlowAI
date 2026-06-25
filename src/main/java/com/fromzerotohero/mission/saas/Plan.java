package com.fromzerotohero.mission.saas;

public enum Plan {
    FREE(25, 10, 25),
    PRO(1_000, 500, 1_000),
    BUSINESS(10_000, 5_000, 10_000);

    private final int workItemLimit;
    private final int monthlyAgentRunLimit;
    private final int monthlyAiAnalysisLimit;

    Plan(int workItemLimit, int monthlyAgentRunLimit, int monthlyAiAnalysisLimit) {
        this.workItemLimit = workItemLimit;
        this.monthlyAgentRunLimit = monthlyAgentRunLimit;
        this.monthlyAiAnalysisLimit = monthlyAiAnalysisLimit;
    }

    public int workItemLimit() { return workItemLimit; }
    public int monthlyAgentRunLimit() { return monthlyAgentRunLimit; }
    public int monthlyAiAnalysisLimit() { return monthlyAiAnalysisLimit; }
}
