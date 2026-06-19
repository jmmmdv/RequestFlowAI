package com.fromzerotohero.mission.intake;

import com.fromzerotohero.mission.saas.TenantOrganization;
import com.fromzerotohero.mission.saas.TenantOrganizationRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestRetentionService {
    private static final Logger log = LoggerFactory.getLogger(RequestRetentionService.class);
    private final TenantOrganizationRepository organizations;
    private final RequestSubmissionRepository submissions;

    public RequestRetentionService(TenantOrganizationRepository organizations,
            RequestSubmissionRepository submissions) {
        this.organizations = organizations;
        this.submissions = submissions;
    }

    @Scheduled(cron = "${mission.intake.retention.cron:0 30 3 * * *}")
    @Transactional
    public void purgeExpiredRequests() {
        Instant now = Instant.now();
        int removed = 0;
        for (TenantOrganization organization : organizations.findAll()) {
            if (!"ACTIVE".equals(organization.getStatus())) continue;
            Instant cutoff = now.minus(organization.getRequestRetentionDays(), ChronoUnit.DAYS);
            removed += submissions.deleteByTenantIdAndCreatedAtBefore(organization.getId(), cutoff);
        }
        if (removed > 0) log.info("Purged {} expired public request submissions", removed);
    }
}
