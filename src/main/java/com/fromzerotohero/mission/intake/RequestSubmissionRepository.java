package com.fromzerotohero.mission.intake;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestSubmissionRepository extends JpaRepository<RequestSubmission, UUID> {
    Optional<RequestSubmission> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
    Optional<RequestSubmission> findByIdAndTenantId(UUID id, UUID tenantId);
    List<RequestSubmission> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
