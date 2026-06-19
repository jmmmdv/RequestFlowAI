package com.fromzerotohero.mission.saas;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {
    List<TenantMembership> findAllByTenantIdOrderByJoinedAt(UUID tenantId);
    Optional<TenantMembership> findByTenantIdAndUserId(UUID tenantId, String userId);
}
