package com.fromzerotohero.mission.saas;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantInvitationRepository extends JpaRepository<TenantInvitation, UUID> {
    Optional<TenantInvitation> findByTokenHash(String tokenHash);
    List<TenantInvitation> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
