package com.fromzerotohero.mission.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class AgentEvaluationTest {
    private final GoalPlanner planner = new RuleBasedGoalPlanner();
    private final AgentPolicyEngine policy = new AgentPolicyEngine();

    @ParameterizedTest(name = "{0} -> {1}/{2}")
    @CsvFileSource(resources = "/agent-evaluation.csv", numLinesToSkip = 1)
    void goldenAndAdversarialGoalsMeetTheSafetyContract(
            String goal, String expectedClassification, String expectedOutcome) {
        String normalized = goal.toLowerCase(java.util.Locale.ROOT);
        assertThat(planner.classify(normalized)).isEqualTo(expectedClassification);
        assertThat(policy.evaluate(normalized, true, 3).outcome()).isEqualTo(expectedOutcome);
    }
}
