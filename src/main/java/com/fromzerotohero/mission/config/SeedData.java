package com.fromzerotohero.mission.config;

import com.fromzerotohero.mission.workitem.Priority;
import com.fromzerotohero.mission.workitem.WorkItem;
import com.fromzerotohero.mission.workitem.WorkItemRepository;
import com.fromzerotohero.mission.workitem.WorkStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.fromzerotohero.mission.security.TenantContext;

@Configuration
public class SeedData {
    @Bean
    @ConditionalOnProperty(name = "mission.seed.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner seed(WorkItemRepository repository, TenantContext tenantContext) {
        return args -> {
            if (repository.count() == 0) {
                repository.save(new WorkItem("Review the urgent website request",
                        "Confirm the client impact and agree on the next action.", Priority.HIGH, WorkStatus.READY,
                        tenantContext.tenantId()));
                repository.save(new WorkItem("Prepare the monthly campaign brief",
                        "Turn the customer request into a clear, trackable deliverable.", Priority.MEDIUM, WorkStatus.BACKLOG,
                        tenantContext.tenantId()));
            }
        };
    }
}
