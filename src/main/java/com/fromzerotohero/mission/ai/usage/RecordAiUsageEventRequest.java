package com.fromzerotohero.mission.ai.usage;

import java.math.BigDecimal;
import java.util.UUID;

public record RecordAiUsageEventRequest(
        UUID tenantId,
        String organizationSlug,
        UUID requestId,
        UUID agentRunId,
        AiUsageOperation operation,
        AiAnalysisSource analysisSource,
        String modelName,
        Integer estimatedInputTokens,
        Integer estimatedOutputTokens,
        BigDecimal estimatedCostUsd,
        boolean paidAiUsed,
        boolean fallbackUsed) {}
