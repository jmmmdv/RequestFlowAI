package com.fromzerotohero.mission.agent;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.time.Instant;

public interface AgentRunRepository extends JpaRepository<AgentRun, UUID> {
    List<AgentRun> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<AgentRun> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AgentRun> findLockedByIdAndTenantId(UUID id, UUID tenantId);
    long countByTenantIdAndCreatedAtGreaterThanEqual(UUID tenantId, Instant since);
}
