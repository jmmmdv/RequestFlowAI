package com.fromzerotohero.mission.ai.budget;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "requestflow.ai.budget")
public class AiBudgetProperties {
    private boolean enabled = true;
    private BigDecimal monthlyBudgetUsd = new BigDecimal("50.00");
    private int warningThresholdPercent = 70;
    private int hardStopThresholdPercent = 90;
    private boolean manualReviewAfterHardStop = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public BigDecimal getMonthlyBudgetUsd() { return monthlyBudgetUsd; }
    public void setMonthlyBudgetUsd(BigDecimal monthlyBudgetUsd) { this.monthlyBudgetUsd = monthlyBudgetUsd; }

    public int getWarningThresholdPercent() { return warningThresholdPercent; }
    public void setWarningThresholdPercent(int warningThresholdPercent) {
        this.warningThresholdPercent = warningThresholdPercent;
    }

    public int getHardStopThresholdPercent() { return hardStopThresholdPercent; }
    public void setHardStopThresholdPercent(int hardStopThresholdPercent) {
        this.hardStopThresholdPercent = hardStopThresholdPercent;
    }

    public boolean isManualReviewAfterHardStop() { return manualReviewAfterHardStop; }
    public void setManualReviewAfterHardStop(boolean manualReviewAfterHardStop) {
        this.manualReviewAfterHardStop = manualReviewAfterHardStop;
    }
}
