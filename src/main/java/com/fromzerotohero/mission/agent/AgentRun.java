package com.fromzerotohero.mission.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import java.util.Arrays;
import java.util.List;
import java.time.Instant;
import java.util.UUID;

@Entity
public class AgentRun {
    @Id
    private UUID id;
    @Column(nullable = false, updatable = false)
    private UUID tenantId;
    @Column(nullable = false, updatable = false, length = 160)
    private String userId;
    @Column(nullable = false, updatable = false, length = 64)
    private String correlationId;
    @Column(nullable = false, updatable = false, length = 500)
    private String goal;
    @Column(nullable = false, updatable = false, length = 80)
    private String classification;
    @Column(nullable = false, length = 30)
    private String outcome;
    @Column(nullable = false)
    private int createdWorkItems;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false, updatable = false, length = 80)
    private String idempotencyKey;
    @Column(nullable = false, updatable = false)
    private int toolBudget;
    @Column(nullable = false, length = 240)
    private String workItemIds;
    private Instant approvedAt;
    @Column(length = 160)
    private String approvedBy;
    @Version
    private long version;

    protected AgentRun() {}

    public AgentRun(UUID tenantId, String userId, String correlationId, String goal,
            String classification, String outcome, int createdWorkItems, String idempotencyKey,
            int toolBudget, List<Long> workItemIds) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.userId = userId;
        this.correlationId = correlationId;
        this.goal = goal;
        this.classification = classification;
        this.outcome = outcome;
        this.createdWorkItems = createdWorkItems;
        this.idempotencyKey = idempotencyKey;
        this.toolBudget = toolBudget;
        setWorkItemIds(workItemIds);
    }

    @PrePersist
    void timestamp() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getUserId() { return userId; }
    public String getCorrelationId() { return correlationId; }
    public String getGoal() { return goal; }
    public String getClassification() { return classification; }
    public String getOutcome() { return outcome; }
    public int getCreatedWorkItems() { return createdWorkItems; }
    public Instant getCreatedAt() { return createdAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public int getToolBudget() { return toolBudget; }
    public Instant getApprovedAt() { return approvedAt; }
    public String getApprovedBy() { return approvedBy; }
    public long getVersion() { return version; }

    public List<Long> workItemIdList() {
        if (workItemIds == null || workItemIds.isBlank()) return List.of();
        return Arrays.stream(workItemIds.split(",")).map(Long::valueOf).toList();
    }

    public void approve(String userId, List<Long> ids) {
        outcome = "EXECUTED";
        createdWorkItems = ids.size();
        approvedAt = Instant.now();
        approvedBy = userId;
        setWorkItemIds(ids);
    }

    private void setWorkItemIds(List<Long> ids) {
        workItemIds = ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }
}
