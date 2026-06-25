package com.fromzerotohero.mission.ai.usage;

import com.fromzerotohero.mission.ai.budget.AiBudgetService;
import com.fromzerotohero.mission.ai.budget.AiBudgetStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiUsageEventService {
    private static final Logger log = LoggerFactory.getLogger(AiUsageEventService.class);
    private static final int COST_SCALE = 6;
    private static final RoundingMode COST_ROUNDING = RoundingMode.HALF_UP;

    private final AiUsageEventRepository events;
    private final AiBudgetService budgetService;
    private final Clock clock;

    @Autowired
    public AiUsageEventService(AiUsageEventRepository events, AiBudgetService budgetService) {
        this(events, budgetService, Clock.systemUTC());
    }

    AiUsageEventService(AiUsageEventRepository events, AiBudgetService budgetService, Clock clock) {
        this.events = events;
        this.budgetService = budgetService;
        this.clock = clock;
    }

    @Transactional
    public AiUsageEvent record(RecordAiUsageEventRequest request) {
        BigDecimal estimatedCostUsd = resolveEstimatedCostUsd(request);
        AiRecordedBudgetStatus budgetStatus = request.budgetStatusOverride() != null
                ? request.budgetStatusOverride()
                : recordedBudgetStatus(budgetService.getBudgetStatus(sumCurrentMonthEstimatedCostUsd()));
        AiUsageEvent event = new AiUsageEvent(request.tenantId(), request.organizationSlug(),
                request.requestId(), request.agentRunId(), request.operation(), request.analysisSource(),
                request.modelName(), request.estimatedInputTokens(), request.estimatedOutputTokens(),
                estimatedCostUsd, budgetStatus, request.paidAiUsed(), request.fallbackUsed());
        return events.save(event);
    }

    public void recordPublicIntakeClassificationSafely(UUID tenantId, String organizationSlug, UUID requestId) {
        try {
            record(new RecordAiUsageEventRequest(tenantId, organizationSlug, requestId, null,
                    AiUsageOperation.PUBLIC_INTAKE_CLASSIFICATION, AiAnalysisSource.RULE_BASED, "RULE_BASED",
                    0, 0, BigDecimal.ZERO, false, false, AiRecordedBudgetStatus.NOT_PAID_AI));
        } catch (Exception exception) {
            log.warn("AI_USAGE_EVENT skipped for public intake tenantId={} requestId={}: {}",
                    tenantId, requestId, exception.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal sumCurrentMonthEstimatedCostUsd() {
        return normalizeCost(events.sumEstimatedCostUsdSince(monthStart()));
    }

    @Transactional(readOnly = true)
    public BigDecimal sumCurrentMonthEstimatedCostUsd(UUID tenantId) {
        return normalizeCost(events.sumEstimatedCostUsdByTenantSince(tenantId, monthStart()));
    }

    @Transactional(readOnly = true)
    public long countCurrentMonthEvents(UUID tenantId) {
        return events.countByTenantIdAndCreatedAtGreaterThanEqual(tenantId, monthStart());
    }

    @Transactional(readOnly = true)
    public List<AiUsageEvent> recentEvents(UUID tenantId) {
        return events.findTop20ByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    private BigDecimal resolveEstimatedCostUsd(RecordAiUsageEventRequest request) {
        if (!request.paidAiUsed()) {
            return BigDecimal.ZERO.setScale(COST_SCALE, COST_ROUNDING);
        }
        return normalizeCost(request.estimatedCostUsd());
    }

    private AiRecordedBudgetStatus recordedBudgetStatus(AiBudgetStatus status) {
        if (!status.enabled()) {
            return AiRecordedBudgetStatus.DISABLED;
        }
        if (status.hardStopReached()) {
            return AiRecordedBudgetStatus.HARD_STOP;
        }
        if (status.warningReached()) {
            return AiRecordedBudgetStatus.WARNING;
        }
        return AiRecordedBudgetStatus.OK;
    }

    private BigDecimal normalizeCost(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            return BigDecimal.ZERO.setScale(COST_SCALE, COST_ROUNDING);
        }
        return value.setScale(COST_SCALE, COST_ROUNDING);
    }

    private Instant monthStart() {
        return clock.instant().atZone(ZoneOffset.UTC).with(TemporalAdjusters.firstDayOfMonth())
                .toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
