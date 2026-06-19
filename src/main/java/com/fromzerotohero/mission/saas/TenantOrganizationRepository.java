package com.fromzerotohero.mission.saas;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantOrganizationRepository extends JpaRepository<TenantOrganization, UUID> {
    boolean existsBySlug(String slug);
}
