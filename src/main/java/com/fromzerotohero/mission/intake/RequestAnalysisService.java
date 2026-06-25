package com.fromzerotohero.mission.intake;

import com.fromzerotohero.mission.ai.provider.AiRequestAnalysisProvider;
import com.fromzerotohero.mission.ai.usage.AiAnalysisSource;
import org.springframework.stereotype.Service;

/**
 * Single entry point for public request analysis.
 *
 * <p>Paid-AI path (when a provider returns a result):
 * <ol>
 *   <li>{@code AiRequestAnalysisProvider} — server-side LLM call (disabled by default)</li>
 *   <li>{@code AiAnalysisQuotaService} — tenant monthly analysis limit</li>
 *   <li>{@code AiBudgetService} — global monthly spend cap and hard stop</li>
 *   <li>{@code AiUsageEventService} — persist paid or fallback usage metadata</li>
 * </ol>
 *
 * <p>Fallback path (active today): {@link RuleBasedRequestClassifier}.
 */
@Service
public class RequestAnalysisService {
    private final AiRequestAnalysisProvider paidAiProvider;
    private final RuleBasedRequestClassifier ruleBasedClassifier;

    public RequestAnalysisService(AiRequestAnalysisProvider paidAiProvider,
            RuleBasedRequestClassifier ruleBasedClassifier) {
        this.paidAiProvider = paidAiProvider;
        this.ruleBasedClassifier = ruleBasedClassifier;
    }

    public RequestAnalysisResult analyze(RequestAnalysisInput input) {
        return paidAiProvider.analyze(input)
                .map(this::fromPaidProvider)
                .orElseGet(() -> ruleBased(ruleBasedClassifier.classify(
                        input.title(), input.details(), input.requestedCategory(), input.requestedUrgency())));
    }

    private RequestAnalysisResult fromPaidProvider(AiRequestAnalysisProvider.AiRequestAnalysisProviderResult result) {
        return new RequestAnalysisResult(result.classification(), AiAnalysisSource.LLM, true, false);
    }

    private RequestAnalysisResult ruleBased(RuleBasedRequestClassifier.Classification classification) {
        return RequestAnalysisResult.ruleBased(classification);
    }
}
