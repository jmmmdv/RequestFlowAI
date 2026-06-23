package com.fromzerotohero.mission.intake;

import com.fromzerotohero.mission.saas.MembershipRole;
import com.fromzerotohero.mission.saas.TenantMembership;
import com.fromzerotohero.mission.saas.TenantMembershipRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class IntakeNotificationService {
    private static final Logger log = LoggerFactory.getLogger(IntakeNotificationService.class);

    private final IntakeNotificationProperties properties;
    private final TenantMembershipRepository memberships;
    private final IntakeNotificationSender sender;
    private final String frontendUrl;

    public IntakeNotificationService(IntakeNotificationProperties properties,
            TenantMembershipRepository memberships, IntakeNotificationSender sender,
            @Value("${mission.frontend-url:http://localhost:8080}") String frontendUrl) {
        this.properties = properties;
        this.memberships = memberships;
        this.sender = sender;
        this.frontendUrl = frontendUrl.replaceAll("/$", "");
    }

    public void notifyNewRequest(NewPublicRequestNotification notification) {
        if (!properties.isEnabled()) {
            return;
        }
        List<String> recipients = resolveRecipients(notification.tenantId());
        if (recipients.isEmpty()) {
            log.warn("INTAKE_NOTIFICATION skipped requestId={} tenantId={}: no admin recipients configured",
                    notification.requestId(), notification.tenantId());
            return;
        }
        try {
            sender.send(recipients, subject(notification), body(notification));
        } catch (RuntimeException exception) {
            log.warn("INTAKE_NOTIFICATION failed requestId={} tenantId={} recipientCount={}: {}",
                    notification.requestId(), notification.tenantId(), recipients.size(),
                    safeMessage(exception));
        }
    }

    NewPublicRequestNotification toNotification(NewPublicRequestEvent event) {
        String dashboardUrl = frontendUrl + "/#workspace";
        return new NewPublicRequestNotification(event.tenantId(), event.organizationName(), event.requestId(),
                event.referenceNumber(), event.requesterName(), event.requesterEmail(), event.companyName(),
                event.title(), event.category(), event.suggestedPriority(), dashboardUrl);
    }

    private List<String> resolveRecipients(UUID tenantId) {
        Set<String> recipients = new LinkedHashSet<>();
        for (TenantMembership membership : memberships.findAllByTenantIdAndRoleOrderByJoinedAtAsc(
                tenantId, MembershipRole.ADMIN)) {
            if (membership.getEmail() != null && !membership.getEmail().isBlank()) {
                recipients.add(membership.getEmail().trim().toLowerCase(Locale.ROOT));
            }
        }
        if (recipients.isEmpty()) {
            String fallback = properties.getFallbackRecipient();
            if (fallback != null && !fallback.isBlank()) {
                recipients.add(fallback.trim().toLowerCase(Locale.ROOT));
            }
        }
        return new ArrayList<>(recipients);
    }

    private String subject(NewPublicRequestNotification notification) {
        return "New request from " + notification.requesterName() + " — " + notification.title();
    }

    private String body(NewPublicRequestNotification notification) {
        StringBuilder body = new StringBuilder();
        body.append("A new request arrived for ").append(notification.organizationName()).append(".\n\n");
        body.append("Requester: ").append(notification.requesterName()).append('\n');
        body.append("Email: ").append(notification.requesterEmail()).append('\n');
        body.append("Company: ").append(notification.companyName()).append('\n');
        body.append("Title: ").append(notification.title()).append('\n');
        body.append("Category: ").append(displayCategory(notification.category())).append('\n');
        body.append("Suggested priority: ").append(notification.suggestedPriority().name()).append('\n');
        body.append("Reference: ").append(notification.referenceNumber()).append('\n');
        body.append("Open dashboard: ").append(notification.dashboardUrl()).append('\n');
        return body.toString();
    }

    private String displayCategory(RequestCategory category) {
        return category == null ? "Not specified" : category.name().replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
