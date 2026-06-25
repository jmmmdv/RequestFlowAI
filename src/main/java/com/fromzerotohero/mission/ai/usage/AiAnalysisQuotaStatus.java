package com.fromzerotohero.mission.ai.usage;

import com.fromzerotohero.mission.saas.Plan;
import java.time.Instant;

public record AiAnalysisQuotaStatus(
        Plan plan,
        long aiAnalysesUsed,
        int aiAnalysesLimit,
        boolean underLimit,
        boolean exceeded,
        Instant periodStartedAt) {}
