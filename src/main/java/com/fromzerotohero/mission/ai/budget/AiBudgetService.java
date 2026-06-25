package com.fromzerotohero.mission.ai.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class AiBudgetService {
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    private final AiBudgetProperties properties;

    public AiBudgetService(AiBudgetProperties properties) {
        this.properties = properties;
    }

    public BigDecimal warningThresholdAmount() {
        return thresholdAmount(properties.getWarningThresholdPercent());
    }

    public BigDecimal hardStopThresholdAmount() {
        return thresholdAmount(properties.getHardStopThresholdPercent());
    }

    public boolean isUnderBudget(BigDecimal currentMonthlySpendUsd) {
        return normalizeSpend(currentMonthlySpendUsd).compareTo(normalizeBudget(properties.getMonthlyBudgetUsd())) < 0;
    }

    public boolean isWarningReached(BigDecimal currentMonthlySpendUsd) {
        if (!properties.isEnabled()) {
            return false;
        }
        return normalizeSpend(currentMonthlySpendUsd).compareTo(warningThresholdAmount()) >= 0;
    }

    public boolean isHardStopReached(BigDecimal currentMonthlySpendUsd) {
        if (!properties.isEnabled()) {
            return false;
        }
        return normalizeSpend(currentMonthlySpendUsd).compareTo(hardStopThresholdAmount()) >= 0;
    }

    public boolean canUsePaidAi(BigDecimal currentMonthlySpendUsd) {
        if (!properties.isEnabled()) {
            return true;
        }
        return !isHardStopReached(currentMonthlySpendUsd);
    }

    public AiBudgetStatus getBudgetStatus(BigDecimal currentMonthlySpendUsd) {
        BigDecimal spend = normalizeSpend(currentMonthlySpendUsd);
        BigDecimal monthlyBudget = normalizeBudget(properties.getMonthlyBudgetUsd());
        BigDecimal warningThreshold = warningThresholdAmount();
        BigDecimal hardStopThreshold = hardStopThresholdAmount();
        boolean warningReached = isWarningReached(spend);
        boolean hardStopReached = isHardStopReached(spend);
        boolean canUsePaidAi = canUsePaidAi(spend);
        boolean manualReviewRequired = hardStopReached && properties.isManualReviewAfterHardStop();
        return new AiBudgetStatus(properties.isEnabled(), monthlyBudget, spend, warningThreshold,
                hardStopThreshold, warningReached, hardStopReached, canUsePaidAi, manualReviewRequired);
    }

    private BigDecimal thresholdAmount(int percent) {
        BigDecimal normalizedPercent = BigDecimal.valueOf(Math.max(0, percent))
                .divide(BigDecimal.valueOf(100), 4, MONEY_ROUNDING);
        return normalizeBudget(properties.getMonthlyBudgetUsd()).multiply(normalizedPercent)
                .setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    private BigDecimal normalizeSpend(BigDecimal currentMonthlySpendUsd) {
        if (currentMonthlySpendUsd == null || currentMonthlySpendUsd.signum() < 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);
        }
        return currentMonthlySpendUsd.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    private BigDecimal normalizeBudget(BigDecimal monthlyBudgetUsd) {
        if (monthlyBudgetUsd == null || monthlyBudgetUsd.signum() <= 0) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING);
        }
        return monthlyBudgetUsd.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }
}
