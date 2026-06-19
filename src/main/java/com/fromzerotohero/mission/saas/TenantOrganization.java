package com.fromzerotohero.mission.saas;

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
@Table(name = "tenant")
public class TenantOrganization {
    @Id private UUID id;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false, unique = true, length = 80) private String slug;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Plan plan;
    @Column(nullable = false, length = 20) private String status;

    protected TenantOrganization() {}

    public TenantOrganization(UUID id, String name, String slug) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.plan = Plan.FREE;
        this.status = "ACTIVE";
    }

    @PrePersist void timestamp() { if (createdAt == null) createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public Instant getCreatedAt() { return createdAt; }
    public Plan getPlan() { return plan; }
    public String getStatus() { return status; }
    public void rename(String name, String slug) { this.name = name; this.slug = slug; }
    public void changePlan(Plan plan) { this.plan = plan; }
}
