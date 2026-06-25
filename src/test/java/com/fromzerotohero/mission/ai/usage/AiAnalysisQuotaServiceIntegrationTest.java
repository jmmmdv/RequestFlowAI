package com.fromzerotohero.mission.ai.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fromzerotohero.mission.intake.RequestSubmission;
import com.fromzerotohero.mission.intake.RequestSubmissionRepository;
import com.fromzerotohero.mission.intake.RuleBasedRequestClassifier;
import com.fromzerotohero.mission.saas.Plan;
import com.fromzerotohero.mission.saas.QuotaExceededException;
import com.fromzerotohero.mission.saas.TenantOrganizationRepository;
import com.fromzerotohero.mission.workitem.WorkItem;
import com.fromzerotohero.mission.workitem.WorkItemRepository;
import com.fromzerotohero.mission.workitem.WorkStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "mission.seed.enabled=false")
class AiAnalysisQuotaServiceIntegrationTest {
    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired AiAnalysisQuotaService quotaService;
    @Autowired AiUsageEventRepository usageEvents;
    @Autowired TenantOrganizationRepository organizations;
    @Autowired RequestSubmissionRepository submissions;
    @Autowired WorkItemRepository workItems;
    @Autowired RuleBasedRequestClassifier classifier;

    @BeforeEach
    void cleanState() {
        usageEvents.deleteAll();
        submissions.deleteAll();
        workItems.deleteAll();
        var organization = organizations.findById(TENANT).orElseThrow();
        organization.changePlan(Plan.FREE);
        organizations.save(organization);
    }

    @Test
    void countsMonthlyAiAnalysesFromUsageEvents() {
        recordAnalysis(3);

        assertThat(quotaService.countMonthlyAiAnalyses(TENANT)).isEqualTo(3);
    }

    @Test
    void deduplicatesRepeatedRequestIdsWithinTheMonth() {
        UUID requestId = createSubmission();
        usageEvents.save(analysisEvent(requestId));
        usageEvents.save(analysisEvent(requestId));

        assertThat(quotaService.countMonthlyAiAnalyses(TENANT)).isOne();
    }

    @Test
    void freePlanQuotaIsUnderLimitBeforeTwentyFiveAnalyses() {
        recordAnalysis(24);

        AiAnalysisQuotaStatus status = quotaService.quotaStatus(TENANT);

        assertThat(status.plan()).isEqualTo(Plan.FREE);
        assertThat(status.aiAnalysesLimit()).isEqualTo(25);
        assertThat(status.aiAnalysesUsed()).isEqualTo(24);
        assertThat(status.underLimit()).isTrue();
        assertThat(status.exceeded()).isFalse();
        assertThat(quotaService.isUnderMonthlyAiAnalysisLimit(TENANT)).isTrue();
    }

    @Test
    void freePlanQuotaIsExceededAtTwentyFiveAnalyses() {
        recordAnalysis(25);

        AiAnalysisQuotaStatus status = quotaService.quotaStatus(TENANT);

        assertThat(status.aiAnalysesUsed()).isEqualTo(25);
        assertThat(status.underLimit()).isFalse();
        assertThat(status.exceeded()).isTrue();
        assertThatThrownBy(() -> quotaService.assertAiAnalysisCapacity(TENANT))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("25");
    }

    @Test
    void proPlanUsesOneThousandMonthlyAiAnalysisLimit() {
        changePlan(Plan.PRO);
        recordAnalysis(999);

        AiAnalysisQuotaStatus status = quotaService.quotaStatus(TENANT);

        assertThat(status.plan()).isEqualTo(Plan.PRO);
        assertThat(status.aiAnalysesLimit()).isEqualTo(1_000);
        assertThat(status.underLimit()).isTrue();

        recordAnalysis(1);
        assertThat(quotaService.quotaStatus(TENANT).exceeded()).isTrue();
    }

    @Test
    void businessPlanUsesTenThousandMonthlyAiAnalysisLimit() {
        changePlan(Plan.BUSINESS);

        assertThat(quotaService.quotaStatus(TENANT).aiAnalysesLimit()).isEqualTo(10_000);
    }

    private void changePlan(Plan plan) {
        var organization = organizations.findById(TENANT).orElseThrow();
        organization.changePlan(plan);
        organizations.saveAndFlush(organization);
    }

    private void recordAnalysis(int count) {
        for (int index = 0; index < count; index++) {
            usageEvents.save(analysisEvent(null));
        }
    }

    private UUID createSubmission() {
        var classification = classifier.classify("Quota test", "Enough details for request submission seed.",
                null, null);
        WorkItem workItem = workItems.save(new WorkItem("Quota test", classification.internalSummary(),
                classification.priority(), WorkStatus.BACKLOG, TENANT));
        RequestSubmission submission = new RequestSubmission(TENANT, "quota-" + UUID.randomUUID(), "Taylor",
                "taylor@example.com", "Taylor Studio", "Quota test",
                "Enough details for request submission seed.", null, null, classification, workItem.getId());
        return submissions.save(submission).getId();
    }

    private AiUsageEvent analysisEvent(UUID requestId) {
        return new AiUsageEvent(TENANT, "local", requestId, null,
                AiUsageOperation.PUBLIC_INTAKE_CLASSIFICATION, AiAnalysisSource.RULE_BASED, "RULE_BASED",
                0, 0, BigDecimal.ZERO, AiRecordedBudgetStatus.NOT_PAID_AI, false, false);
    }
}
