package com.fromzerotohero.mission.ai.usage;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "mission.seed.enabled=false")
class AiUsageEventIntegrationTest {
    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired AiUsageEventService usageEvents;
    @Autowired AiUsageEventRepository repository;

    @BeforeEach
    void cleanState() {
        repository.deleteAll();
    }

    @Test
    void recordsZeroCostRuleBasedEvent() {
        AiUsageEvent event = usageEvents.record(ruleBasedRequest("local", null));

        assertThat(event.getAnalysisSource()).isEqualTo(AiAnalysisSource.RULE_BASED);
        assertThat(event.isPaidAiUsed()).isFalse();
        assertThat(event.getEstimatedCostUsd()).isEqualByComparingTo("0.000000");
        assertThat(event.getBudgetStatus()).isEqualTo(AiRecordedBudgetStatus.OK);
        assertThat(event.getOperation()).isEqualTo(AiUsageOperation.REQUEST_ANALYSIS);
        assertThat(event.getOrganizationSlug()).isEqualTo("local");
    }

    @Test
    void paidAiFalseDoesNotPersistNonZeroCost() {
        AiUsageEvent event = usageEvents.record(new RecordAiUsageEventRequest(
                TENANT, "local", null, null, AiUsageOperation.REQUEST_ANALYSIS,
                AiAnalysisSource.RULE_BASED, null, null, null, new BigDecimal("1.250000"),
                false, false, null));

        assertThat(event.getEstimatedCostUsd()).isEqualByComparingTo("0.000000");
        assertThat(usageEvents.sumCurrentMonthEstimatedCostUsd()).isEqualByComparingTo("0.000000");
    }

    @Test
    void sumsMonthlyEstimatedCostForPaidAiEvents() {
        usageEvents.record(ruleBasedRequest("local", null));
        usageEvents.record(new RecordAiUsageEventRequest(
                TENANT, "local", null, null, AiUsageOperation.REQUEST_ANALYSIS,
                AiAnalysisSource.LLM, "gpt-4o-mini", 120, 40, new BigDecimal("0.015000"),
                true, false, null));
        usageEvents.record(new RecordAiUsageEventRequest(
                TENANT, "local", null, null, AiUsageOperation.AGENT_ANALYSIS,
                AiAnalysisSource.LLM, "gpt-4o-mini", 80, 20, new BigDecimal("0.010000"),
                true, false, null));

        assertThat(usageEvents.sumCurrentMonthEstimatedCostUsd()).isEqualByComparingTo("0.025000");
        assertThat(usageEvents.sumCurrentMonthEstimatedCostUsd(TENANT)).isEqualByComparingTo("0.025000");
        assertThat(usageEvents.countCurrentMonthEvents(TENANT)).isEqualTo(3);
        assertThat(usageEvents.recentEvents(TENANT)).hasSize(3);
    }

    @Test
    void recordsFallbackEventWithoutPaidAiCost() {
        AiUsageEvent event = usageEvents.record(new RecordAiUsageEventRequest(
                TENANT, "local", null, null, AiUsageOperation.REQUEST_ANALYSIS,
                AiAnalysisSource.FALLBACK, null, null, null, new BigDecimal("0.500000"),
                false, true, null));

        assertThat(event.isFallbackUsed()).isTrue();
        assertThat(event.getEstimatedCostUsd()).isEqualByComparingTo("0.000000");
        assertThat(usageEvents.sumCurrentMonthEstimatedCostUsd()).isEqualByComparingTo("0.000000");
    }

    private RecordAiUsageEventRequest ruleBasedRequest(String slug, UUID requestId) {
        return new RecordAiUsageEventRequest(TENANT, slug, requestId, null,
                AiUsageOperation.REQUEST_ANALYSIS, AiAnalysisSource.RULE_BASED, null,
                null, null, BigDecimal.ZERO, false, false, null);
    }
}
