package com.fromzerotohero.mission.ai.usage;

import com.fromzerotohero.mission.saas.Plan;
import com.fromzerotohero.mission.saas.QuotaExceededException;
import com.fromzerotohero.mission.saas.TenantOrganization;
import com.fromzerotohero.mission.saas.TenantOrganizationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiAnalysisQuotaService {
    static final List<AiUsageOperation> ANALYSIS_OPERATIONS = List.of(
            AiUsageOperation.PUBLIC_INTAKE_CLASSIFICATION,
            AiUsageOperation.REQUEST_ANALYSIS,
            AiUsageOperation.AGENT_ANALYSIS);

    private final TenantOrganizationRepository organizations;
    private final AiUsageEventRepository usageEvents;
    private final Clock clock;

    @Autowired
    public AiAnalysisQuotaService(TenantOrganizationRepository organizations,
            AiUsageEventRepository usageEvents) {
        this(organizations, usageEvents, Clock.systemUTC());
    }

    AiAnalysisQuotaService(TenantOrganizationRepository organizations, AiUsageEventRepository usageEvents,
            Clock clock) {
        this.organizations = organizations;
        this.usageEvents = usageEvents;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public long countMonthlyAiAnalyses(UUID tenantId) {
        return usageEvents.countMonthlyAiAnalysesByTenantSince(tenantId, monthStart(), ANALYSIS_OPERATIONS);
    }

    @Transactional(readOnly = true)
    public boolean isUnderMonthlyAiAnalysisLimit(UUID tenantId) {
        return quotaStatus(tenantId).underLimit();
    }

    @Transactional(readOnly = true)
    public AiAnalysisQuotaStatus quotaStatus(UUID tenantId) {
        Plan plan = plan(tenantId);
        long used = countMonthlyAiAnalyses(tenantId);
        int limit = plan.monthlyAiAnalysisLimit();
        boolean underLimit = used < limit;
        return new AiAnalysisQuotaStatus(plan, used, limit, underLimit, !underLimit, monthStart());
    }

    public void assertAiAnalysisCapacity(UUID tenantId) {
        AiAnalysisQuotaStatus status = quotaStatus(tenantId);
        if (status.exceeded()) {
            throw new QuotaExceededException("AI analyses per month", status.aiAnalysesLimit());
        }
    }

    private Plan plan(UUID tenantId) {
        return organizations.findById(tenantId).map(TenantOrganization::getPlan).orElse(Plan.FREE);
    }

    private Instant monthStart() {
        return clock.instant().atZone(ZoneOffset.UTC).with(TemporalAdjusters.firstDayOfMonth())
                .toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
