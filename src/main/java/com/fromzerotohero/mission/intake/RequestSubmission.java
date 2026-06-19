package com.fromzerotohero.mission.intake;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fromzerotohero.mission.workitem.Priority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "request_submission")
public class RequestSubmission {
    @Id private UUID id;
    @JsonIgnore @Column(nullable = false, updatable = false) private UUID tenantId;
    @JsonIgnore @Column(nullable = false, updatable = false, length = 80) private String idempotencyKey;
    @Column(nullable = false, length = 120) private String requesterName;
    @Column(nullable = false, length = 254) private String requesterEmail;
    @Column(nullable = false, length = 120) private String title;
    @Column(nullable = false, length = 2000) private String details;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private RequestCategory category;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Priority suggestedPriority;
    @Column(nullable = false, length = 500) private String internalSummary;
    @Column(nullable = false, length = 500) private String recommendedNextAction;
    @Column(nullable = false, length = 20) private String status;
    private Long workItemId;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    protected RequestSubmission() {}

    public RequestSubmission(UUID tenantId, String idempotencyKey, String requesterName,
            String requesterEmail, String title, String details,
            RuleBasedRequestClassifier.Classification classification, Long workItemId) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.idempotencyKey = idempotencyKey;
        this.requesterName = requesterName;
        this.requesterEmail = requesterEmail;
        this.title = title;
        this.details = details;
        this.category = classification.category();
        this.suggestedPriority = classification.priority();
        this.internalSummary = classification.internalSummary();
        this.recommendedNextAction = classification.recommendedNextAction();
        this.status = "RECEIVED";
        this.workItemId = workItemId;
    }

    @PrePersist void timestamp() { if (createdAt == null) createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getRequesterName() { return requesterName; }
    public String getRequesterEmail() { return requesterEmail; }
    public String getTitle() { return title; }
    public String getDetails() { return details; }
    public RequestCategory getCategory() { return category; }
    public Priority getSuggestedPriority() { return suggestedPriority; }
    public String getInternalSummary() { return internalSummary; }
    public String getRecommendedNextAction() { return recommendedNextAction; }
    public String getStatus() { return status; }
    public Long getWorkItemId() { return workItemId; }
    public Instant getCreatedAt() { return createdAt; }
}
