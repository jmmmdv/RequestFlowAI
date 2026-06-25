package com.fromzerotohero.mission.intake;

import com.fromzerotohero.mission.ai.usage.AiAnalysisSource;

public record RequestAnalysisResult(
        RuleBasedRequestClassifier.Classification classification,
        AiAnalysisSource analysisSource,
        boolean paidAiUsed,
        boolean fallbackUsed) {

    public static RequestAnalysisResult ruleBased(RuleBasedRequestClassifier.Classification classification) {
        return new RequestAnalysisResult(classification, AiAnalysisSource.RULE_BASED, false, false);
    }
}
