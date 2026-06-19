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
public class TenantInvitation {
    @Id private UUID id;
    @Column(nullable = false, updatable = false) private UUID tenantId;
    @Column(nullable = false, updatable = false, length = 254) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false, updatable = false, length = 20) private MembershipRole role;
    @Column(nullable = false, unique = true, updatable = false, length = 64) private String tokenHash;
    @Column(nullable = false, updatable = false, length = 160) private String invitedBy;
    @Column(nullable = false, updatable = false) private Instant expiresAt;
    private Instant acceptedAt;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    protected TenantInvitation() {}
    public TenantInvitation(UUID tenantId, String email, MembershipRole role, String tokenHash,
            String invitedBy, Instant expiresAt) {
        this.id = UUID.randomUUID(); this.tenantId = tenantId; this.email = email;
        this.role = role; this.tokenHash = tokenHash; this.invitedBy = invitedBy; this.expiresAt = expiresAt;
    }
    @PrePersist void timestamp() { createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getEmail() { return email; }
    public MembershipRole getRole() { return role; }
    public String getInvitedBy() { return invitedBy; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isUsable(Instant now) { return acceptedAt == null && expiresAt.isAfter(now); }
    public void accept() { acceptedAt = Instant.now(); }
}
