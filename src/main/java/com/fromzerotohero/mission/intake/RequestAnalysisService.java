package com.fromzerotohero.mission.intake;

import org.springframework.stereotype.Service;

/**
 * Single entry point for public request analysis.
 *
 * <p>Future paid-AI path (not implemented):
 * <ol>
 *   <li>{@code AiAnalysisQuotaService} — tenant monthly analysis limit</li>
 *   <li>{@code AiBudgetService} — global monthly spend cap and hard stop</li>
 *   <li>LLM provider call — server-side only, no browser keys</li>
 *   <li>{@code AiUsageEventService} — persist paid or fallback usage metadata</li>
 *   <li>{@link RuleBasedRequestClassifier} — deterministic fallback when budget, quota, or provider fails</li>
 * </ol>
 */
@Service
public class RequestAnalysisService {
    private final RuleBasedRequestClassifier ruleBasedClassifier;

    public RequestAnalysisService(RuleBasedRequestClassifier ruleBasedClassifier) {
        this.ruleBasedClassifier = ruleBasedClassifier;
    }

    public RequestAnalysisResult analyze(RequestAnalysisInput input) {
        RuleBasedRequestClassifier.Classification classification = ruleBasedClassifier.classify(
                input.title(), input.details(), input.requestedCategory(), input.requestedUrgency());
        return RequestAnalysisResult.ruleBased(classification);
    }
}
