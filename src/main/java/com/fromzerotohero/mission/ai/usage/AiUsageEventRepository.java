package com.fromzerotohero.mission.ai.usage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiUsageEventRepository extends JpaRepository<AiUsageEvent, UUID> {
    @Query("""
            select coalesce(sum(event.estimatedCostUsd), 0)
            from AiUsageEvent event
            where event.createdAt >= :since
            """)
    BigDecimal sumEstimatedCostUsdSince(@Param("since") Instant since);

    @Query("""
            select coalesce(sum(event.estimatedCostUsd), 0)
            from AiUsageEvent event
            where event.tenantId = :tenantId and event.createdAt >= :since
            """)
    BigDecimal sumEstimatedCostUsdByTenantSince(@Param("tenantId") UUID tenantId, @Param("since") Instant since);

    long countByTenantIdAndCreatedAtGreaterThanEqual(UUID tenantId, Instant since);

    @Query("""
            select count(distinct coalesce(event.requestId, event.id))
            from AiUsageEvent event
            where event.tenantId = :tenantId
              and event.createdAt >= :since
              and event.operation in :operations
            """)
    long countMonthlyAiAnalysesByTenantSince(@Param("tenantId") UUID tenantId, @Param("since") Instant since,
            @Param("operations") List<AiUsageOperation> operations);

    List<AiUsageEvent> findTop20ByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
