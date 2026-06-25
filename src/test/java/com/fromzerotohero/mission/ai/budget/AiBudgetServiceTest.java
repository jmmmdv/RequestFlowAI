package com.fromzerotohero.mission.ai.budget;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiBudgetServiceTest {
    private AiBudgetProperties properties;
    private AiBudgetService service;

    @BeforeEach
    void setUp() {
        properties = new AiBudgetProperties();
        service = new AiBudgetService(properties);
    }

    @Test
    void belowWarningThresholdAllowsPaidAi() {
        AiBudgetStatus status = service.getBudgetStatus(new BigDecimal("34.99"));

        assertThat(status.warningThresholdUsd()).isEqualByComparingTo("35.00");
        assertThat(status.hardStopThresholdUsd()).isEqualByComparingTo("45.00");
        assertThat(status.warningReached()).isFalse();
        assertThat(status.hardStopReached()).isFalse();
        assertThat(status.canUsePaidAi()).isTrue();
        assertThat(status.manualReviewRequired()).isFalse();
        assertThat(service.isUnderBudget(new BigDecimal("34.99"))).isTrue();
    }

    @Test
    void warningReachedAtSeventyPercent() {
        BigDecimal spend = new BigDecimal("35.00");

        assertThat(service.isWarningReached(spend)).isTrue();
        assertThat(service.isHardStopReached(spend)).isFalse();
        assertThat(service.canUsePaidAi(spend)).isTrue();

        AiBudgetStatus status = service.getBudgetStatus(spend);
        assertThat(status.warningReached()).isTrue();
        assertThat(status.hardStopReached()).isFalse();
        assertThat(status.canUsePaidAi()).isTrue();
    }

    @Test
    void hardStopReachedAtNinetyPercent() {
        BigDecimal spend = new BigDecimal("45.00");

        assertThat(service.isHardStopReached(spend)).isTrue();
        assertThat(service.isWarningReached(spend)).isTrue();

        AiBudgetStatus status = service.getBudgetStatus(spend);
        assertThat(status.hardStopReached()).isTrue();
        assertThat(status.warningReached()).isTrue();
    }

    @Test
    void canUsePaidAiIsFalseAfterHardStop() {
        assertThat(service.canUsePaidAi(new BigDecimal("45.00"))).isFalse();
        assertThat(service.canUsePaidAi(new BigDecimal("50.00"))).isFalse();
        assertThat(service.getBudgetStatus(new BigDecimal("45.00")).canUsePaidAi()).isFalse();
    }

    @Test
    void manualReviewRequiredAfterHardStopWhenConfigured() {
        properties.setManualReviewAfterHardStop(true);

        assertThat(service.getBudgetStatus(new BigDecimal("45.00")).manualReviewRequired()).isTrue();
    }

    @Test
    void manualReviewNotRequiredWhenDisabled() {
        properties.setManualReviewAfterHardStop(false);

        assertThat(service.getBudgetStatus(new BigDecimal("45.00")).manualReviewRequired()).isFalse();
    }

    @Test
    void budgetDisabledDoesNotBlockPaidAi() {
        properties.setEnabled(false);

        AiBudgetStatus status = service.getBudgetStatus(new BigDecimal("100.00"));

        assertThat(status.enabled()).isFalse();
        assertThat(status.warningReached()).isFalse();
        assertThat(status.hardStopReached()).isFalse();
        assertThat(status.canUsePaidAi()).isTrue();
        assertThat(status.manualReviewRequired()).isFalse();
    }
}
