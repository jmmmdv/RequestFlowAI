package com.fromzerotohero.mission.ai.provider;

import com.fromzerotohero.mission.intake.RequestAnalysisInput;
import com.fromzerotohero.mission.intake.RuleBasedRequestClassifier;
import java.math.BigDecimal;
import java.util.Optional;

public interface AiRequestAnalysisProvider {
  /**
   * Attempt paid AI analysis. Returns empty when the provider is disabled, unavailable, or fails.
   */
  Optional<AiRequestAnalysisProviderResult> analyze(RequestAnalysisInput input);

  record AiRequestAnalysisProviderResult(
      RuleBasedRequestClassifier.Classification classification,
      String modelName,
      Integer estimatedInputTokens,
      Integer estimatedOutputTokens,
      BigDecimal estimatedCostUsd) {}
}
