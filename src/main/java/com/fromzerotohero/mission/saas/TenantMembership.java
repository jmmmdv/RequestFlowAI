package com.fromzerotohero.mission.saas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import java.time.Instant;
import java.util.UUID;

@Entity
public class TenantMembership {
    @Id private UUID id;
    @Column(nullable = false, updatable = false) private UUID tenantId;
    @Column(nullable = false, updatable = false, length = 160) private String userId;
    @Column(length = 254) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private MembershipRole role;
    @Column(nullable = false, updatable = false) private Instant joinedAt;

    protected TenantMembership() {}
    public TenantMembership(UUID tenantId, String userId, String email, MembershipRole role) {
        this.id = UUID.randomUUID(); this.tenantId = tenantId; this.userId = userId;
        this.email = email; this.role = role;
    }
    @PrePersist void timestamp() { joinedAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public MembershipRole getRole() { return role; }
    public Instant getJoinedAt() { return joinedAt; }
}
