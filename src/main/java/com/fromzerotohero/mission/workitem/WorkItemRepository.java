package com.fromzerotohero.mission.workitem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {
    List<WorkItem> findAllByTenantIdOrderByUpdatedAtDesc(UUID tenantId);
    Optional<WorkItem> findByIdAndTenantId(Long id, UUID tenantId);
    long countByTenantId(UUID tenantId);
}
