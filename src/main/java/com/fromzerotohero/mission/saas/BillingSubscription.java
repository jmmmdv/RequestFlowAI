package com.fromzerotohero.mission.saas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.util.UUID;

@Entity
public class BillingSubscription {
    @Id private UUID tenantId;
    @Column(length = 120) private String stripeCustomerId;
    @Column(unique = true, length = 120) private String stripeSubscriptionId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Plan plan;
    @Column(nullable = false, length = 30) private String status;
    private Instant currentPeriodEnd;
    @Column(nullable = false) private Instant updatedAt;

    protected BillingSubscription() {}
    public BillingSubscription(UUID tenantId) { this.tenantId = tenantId; this.plan = Plan.FREE; this.status = "ACTIVE"; }
    @PrePersist @PreUpdate void timestamp() { updatedAt = Instant.now(); }
    public UUID getTenantId() { return tenantId; }
    public String getStripeCustomerId() { return stripeCustomerId; }
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public Plan getPlan() { return plan; }
    public String getStatus() { return status; }
    public Instant getCurrentPeriodEnd() { return currentPeriodEnd; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void synchronize(String customerId, String subscriptionId, Plan plan, String status, Instant periodEnd) {
        this.stripeCustomerId = customerId; this.stripeSubscriptionId = subscriptionId;
        this.plan = plan; this.status = status; this.currentPeriodEnd = periodEnd;
    }
}
