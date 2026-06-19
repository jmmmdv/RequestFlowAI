package com.fromzerotohero.mission.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
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
    @Column(nullable = false, updatable = false, length = 30)
    private String outcome;
    @Column(nullable = false, updatable = false)
    private int createdWorkItems;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AgentRun() {}

    public AgentRun(UUID tenantId, String userId, String correlationId, String goal,
            String classification, String outcome, int createdWorkItems) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.userId = userId;
        this.correlationId = correlationId;
        this.goal = goal;
        this.classification = classification;
        this.outcome = outcome;
        this.createdWorkItems = createdWorkItems;
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
}
