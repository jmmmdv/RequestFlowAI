package com.fromzerotohero.mission.intake;

import com.fromzerotohero.mission.workitem.Priority;
import java.util.UUID;

public record NewPublicRequestNotification(
        UUID tenantId,
        String organizationName,
        UUID requestId,
        String referenceNumber,
        String requesterName,
        String requesterEmail,
        String companyName,
        String title,
        RequestCategory category,
        Priority suggestedPriority,
        String dashboardUrl) {}
