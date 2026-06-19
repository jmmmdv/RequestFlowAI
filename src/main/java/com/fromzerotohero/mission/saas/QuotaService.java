package com.fromzerotohero.mission.saas;

import com.fromzerotohero.mission.agent.AgentRunRepository;
import com.fromzerotohero.mission.workitem.WorkItemRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class QuotaService {
    private final TenantOrganizationRepository organizations;
    private final WorkItemRepository workItems;
    private final AgentRunRepository agentRuns;
    private final Clock clock;

    @Autowired
    public QuotaService(TenantOrganizationRepository organizations, WorkItemRepository workItems,
            AgentRunRepository agentRuns) {
        this(organizations, workItems, agentRuns, Clock.systemUTC());
    }

    QuotaService(TenantOrganizationRepository organizations, WorkItemRepository workItems,
            AgentRunRepository agentRuns, Clock clock) {
        this.organizations = organizations; this.workItems = workItems;
        this.agentRuns = agentRuns; this.clock = clock;
    }

    public void assertWorkItemCapacity(UUID tenantId, int additional) {
        Plan plan = plan(tenantId);
        if (workItems.countByTenantId(tenantId) + additional > plan.workItemLimit()) {
            throw new QuotaExceededException("work items", plan.workItemLimit());
        }
    }

    public void assertAgentRunCapacity(UUID tenantId) {
        Plan plan = plan(tenantId);
        if (agentRuns.countByTenantIdAndCreatedAtGreaterThanEqual(tenantId, monthStart())
                >= plan.monthlyAgentRunLimit()) {
            throw new QuotaExceededException("agent runs per month", plan.monthlyAgentRunLimit());
        }
    }

    public UsageSnapshot usage(UUID tenantId) {
        Plan plan = plan(tenantId);
        return new UsageSnapshot(plan, workItems.countByTenantId(tenantId), plan.workItemLimit(),
                agentRuns.countByTenantIdAndCreatedAtGreaterThanEqual(tenantId, monthStart()),
                plan.monthlyAgentRunLimit(), monthStart());
    }

    private Plan plan(UUID tenantId) {
        return organizations.findById(tenantId).map(TenantOrganization::getPlan).orElse(Plan.FREE);
    }

    private Instant monthStart() {
        return clock.instant().atZone(ZoneOffset.UTC).with(TemporalAdjusters.firstDayOfMonth())
                .toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public record UsageSnapshot(Plan plan, long workItemsUsed, int workItemsLimit,
            long agentRunsUsed, int agentRunsLimit, Instant periodStartedAt) {}
}
