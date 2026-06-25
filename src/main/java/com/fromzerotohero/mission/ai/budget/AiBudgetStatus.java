package com.fromzerotohero.mission.ai.budget;

import java.math.BigDecimal;

public record AiBudgetStatus(
        boolean enabled,
        BigDecimal monthlyBudgetUsd,
        BigDecimal currentMonthlySpendUsd,
        BigDecimal warningThresholdUsd,
        BigDecimal hardStopThresholdUsd,
        boolean warningReached,
        boolean hardStopReached,
        boolean canUsePaidAi,
        boolean manualReviewRequired) {}
