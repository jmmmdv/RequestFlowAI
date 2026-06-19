package com.fromzerotohero.mission.agent;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AgentPolicyEngine {
    private static final Pattern UNSAFE = Pattern.compile(
            ".*(ignore (all |the )?(previous|system)|system prompt|drop table|delete from|"
                    + "<script|rm -rf|bypass (security|approval)).*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HIGH_IMPACT = Pattern.compile(
            ".*(urgent|production|security|outage|delete|destroy|rotate secret).*",
            Pattern.CASE_INSENSITIVE);

    public Decision evaluate(String goal, boolean executionRequested, int toolBudget) {
        if (UNSAFE.matcher(goal).matches()) {
            return new Decision("BLOCKED", false, "Policy rejected adversarial or destructive instructions");
        }
        if (!executionRequested) {
            return new Decision("DRY_RUN", false, "Dry run requested; no state will change");
        }
        if (toolBudget < 3) {
            return new Decision("BUDGET_EXCEEDED", false,
                    "The three-step plan exceeds the approved tool budget");
        }
        if (HIGH_IMPACT.matcher(goal).matches()) {
            return new Decision("PENDING_APPROVAL", true,
                    "High-impact goal requires administrator approval");
        }
        return new Decision("EXECUTED", false, "Policy permits bounded execution");
    }

    public record Decision(String outcome, boolean approvalRequired, String reason) {}
}
