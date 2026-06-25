package com.fromzerotohero.mission.saas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlanAiAnalysisLimitTest {
    @Test
    void definesMonthlyAiAnalysisLimitsPerPlan() {
        assertThat(Plan.FREE.monthlyAiAnalysisLimit()).isEqualTo(25);
        assertThat(Plan.PRO.monthlyAiAnalysisLimit()).isEqualTo(1_000);
        assertThat(Plan.BUSINESS.monthlyAiAnalysisLimit()).isEqualTo(10_000);
    }
}
