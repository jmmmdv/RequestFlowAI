package com.fromzerotohero.mission.intake;

import com.fromzerotohero.mission.workitem.Priority;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedRequestClassifier {
    public Classification classify(String title, String details) {
        String text = (title + " " + details).toLowerCase(Locale.ROOT);
        RequestCategory category = category(text);
        Priority priority = priority(text);
        return new Classification(category, priority,
                summary(title, details), recommendedNextAction(category, priority));
    }

    private RequestCategory category(String text) {
        if (contains(text, "invoice", "billing", "payment", "refund", "charge", "subscription")) {
            return RequestCategory.BILLING;
        }
        if (contains(text, "bug", "broken", "error", "outage", "down", "not working", "cannot log")) {
            return RequestCategory.SUPPORT;
        }
        if (contains(text, "quote", "pricing", "proposal", "demo", "estimate", "sales")) {
            return RequestCategory.SALES;
        }
        if (contains(text, "change", "update", "add", "build", "design", "campaign", "revise")) {
            return RequestCategory.CHANGE_REQUEST;
        }
        return RequestCategory.GENERAL;
    }

    private Priority priority(String text) {
        if (contains(text, "urgent", "emergency", "outage", "site is down", "security breach",
                "cannot access", "critical")) {
            return Priority.CRITICAL;
        }
        if (contains(text, "asap", "today", "blocked", "deadline", "high priority")) {
            return Priority.HIGH;
        }
        if (contains(text, "no rush", "when available", "question", "information only")) {
            return Priority.LOW;
        }
        return Priority.MEDIUM;
    }

    private String summary(String title, String details) {
        String normalized = details.trim().replaceAll("\\s+", " ");
        String excerpt = normalized.substring(0, Math.min(240, normalized.length()));
        return title + " — " + excerpt + (normalized.length() > excerpt.length() ? "…" : "");
    }

    private String recommendedNextAction(RequestCategory category, Priority priority) {
        String urgency = priority == Priority.CRITICAL
                ? "Acknowledge immediately, assign an owner, and communicate the response time. " : "";
        String action = switch (category) {
            case SUPPORT -> "Confirm the impact, reproduce the issue, and send the requester a status update.";
            case BILLING -> "Review the account and transaction history before replying with the resolution path.";
            case SALES -> "Confirm the requested outcome, budget, and timing, then assign a follow-up owner.";
            case CHANGE_REQUEST -> "Clarify scope and acceptance criteria before estimating and scheduling the work.";
            case GENERAL -> "Confirm the desired outcome and assign the request to the right team member.";
        };
        return urgency + action;
    }

    private boolean contains(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }

    public record Classification(RequestCategory category, Priority priority,
            String internalSummary, String recommendedNextAction) {}
}
