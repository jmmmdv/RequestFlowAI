package com.fromzerotohero.mission.ai.usage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_usage_event")
public class AiUsageEvent {
    @Id
    private UUID id;
    @Column(nullable = false, updatable = false)
    private UUID tenantId;
    @Column(length = 80, updatable = false)
    private String organizationSlug;
    @Column(updatable = false)
    private UUID requestId;
    @Column(updatable = false)
    private UUID agentRunId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 40)
    private AiUsageOperation operation;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private AiAnalysisSource analysisSource;
    @Column(length = 80, updatable = false)
    private String modelName;
    @Column(updatable = false)
    private Integer estimatedInputTokens;
    @Column(updatable = false)
    private Integer estimatedOutputTokens;
    @Column(updatable = false)
    private Integer estimatedTotalTokens;
    @Column(nullable = false, updatable = false, precision = 12, scale = 6)
    private BigDecimal estimatedCostUsd;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private AiRecordedBudgetStatus budgetStatus;
    @Column(nullable = false, updatable = false)
    private boolean paidAiUsed;
    @Column(nullable = false, updatable = false)
    private boolean fallbackUsed;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected AiUsageEvent() {}

    public AiUsageEvent(UUID tenantId, String organizationSlug, UUID requestId, UUID agentRunId,
            AiUsageOperation operation, AiAnalysisSource analysisSource, String modelName,
            Integer estimatedInputTokens, Integer estimatedOutputTokens, BigDecimal estimatedCostUsd,
            AiRecordedBudgetStatus budgetStatus, boolean paidAiUsed, boolean fallbackUsed) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.organizationSlug = organizationSlug;
        this.requestId = requestId;
        this.agentRunId = agentRunId;
        this.operation = operation;
        this.analysisSource = analysisSource;
        this.modelName = modelName;
        this.estimatedInputTokens = estimatedInputTokens;
        this.estimatedOutputTokens = estimatedOutputTokens;
        this.estimatedTotalTokens = totalTokens(estimatedInputTokens, estimatedOutputTokens);
        this.estimatedCostUsd = estimatedCostUsd;
        this.budgetStatus = budgetStatus;
        this.paidAiUsed = paidAiUsed;
        this.fallbackUsed = fallbackUsed;
    }

    @PrePersist
    void timestamp() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    private static Integer totalTokens(Integer inputTokens, Integer outputTokens) {
        if (inputTokens == null && outputTokens == null) {
            return null;
        }
        return (inputTokens == null ? 0 : inputTokens) + (outputTokens == null ? 0 : outputTokens);
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getOrganizationSlug() { return organizationSlug; }
    public UUID getRequestId() { return requestId; }
    public UUID getAgentRunId() { return agentRunId; }
    public AiUsageOperation getOperation() { return operation; }
    public AiAnalysisSource getAnalysisSource() { return analysisSource; }
    public String getModelName() { return modelName; }
    public Integer getEstimatedInputTokens() { return estimatedInputTokens; }
    public Integer getEstimatedOutputTokens() { return estimatedOutputTokens; }
    public Integer getEstimatedTotalTokens() { return estimatedTotalTokens; }
    public BigDecimal getEstimatedCostUsd() { return estimatedCostUsd; }
    public AiRecordedBudgetStatus getBudgetStatus() { return budgetStatus; }
    public boolean isPaidAiUsed() { return paidAiUsed; }
    public boolean isFallbackUsed() { return fallbackUsed; }
    public Instant getCreatedAt() { return createdAt; }
}
