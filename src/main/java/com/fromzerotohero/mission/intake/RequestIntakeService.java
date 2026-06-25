package com.fromzerotohero.mission.intake;

import com.fromzerotohero.mission.ai.usage.AiUsageEventService;
import com.fromzerotohero.mission.saas.QuotaService;
import com.fromzerotohero.mission.saas.SaasException;
import com.fromzerotohero.mission.saas.TenantOrganization;
import com.fromzerotohero.mission.saas.TenantOrganizationRepository;
import com.fromzerotohero.mission.security.TenantContext;
import com.fromzerotohero.mission.workitem.WorkItem;
import com.fromzerotohero.mission.workitem.WorkItemRepository;
import com.fromzerotohero.mission.workitem.WorkStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestIntakeService {
    private final TenantOrganizationRepository organizations;
    private final RequestSubmissionRepository submissions;
    private final WorkItemRepository workItems;
    private final RuleBasedRequestClassifier classifier;
    private final QuotaService quotas;
    private final TenantContext tenantContext;
    private final PublicIntakeProperties properties;
    private final PortalTokenHasher tokenHasher;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final AiUsageEventService usageEvents;

    public RequestIntakeService(TenantOrganizationRepository organizations,
            RequestSubmissionRepository submissions, WorkItemRepository workItems,
            RuleBasedRequestClassifier classifier, QuotaService quotas, TenantContext tenantContext,
            PublicIntakeProperties properties, PortalTokenHasher tokenHasher,
            org.springframework.context.ApplicationEventPublisher events,
            AiUsageEventService usageEvents) {
        this.organizations = organizations;
        this.submissions = submissions;
        this.workItems = workItems;
        this.classifier = classifier;
        this.quotas = quotas;
        this.tenantContext = tenantContext;
        this.properties = properties;
        this.tokenHasher = tokenHasher;
        this.events = events;
        this.usageEvents = usageEvents;
    }

    @Transactional(readOnly = true)
    public Portal portal(String organizationSlug, String portalToken) {
        TenantOrganization organization = activeOrganization(organizationSlug, portalToken);
        return new Portal(organization.getName(), organization.getSlug(), organization.portalTokenRequired());
    }

    @Transactional
    public Receipt submit(String organizationSlug, IntakeRequest request, String requestedKey, String portalToken) {
        assertNotBot(request);
        TenantOrganization organization = activeOrganization(organizationSlug, portalToken);
        String idempotencyKey = normalizeKey(requestedKey);
        var existing = submissions.findByTenantIdAndIdempotencyKey(organization.getId(), idempotencyKey);
        if (existing.isPresent()) return receipt(existing.get(), true);

        String title = normalize(request.title());
        String details = normalize(request.details());
        var classification = classifier.classify(title, details, request.category(), request.urgency());
        quotas.assertWorkItemCapacity(organization.getId(), 1);
        WorkItem workItem = workItems.save(new WorkItem(title, classification.internalSummary(),
                classification.priority(), WorkStatus.BACKLOG, organization.getId()));
        RequestSubmission submission = submissions.save(new RequestSubmission(organization.getId(),
                idempotencyKey, normalize(request.requesterName()), request.requesterEmail().trim().toLowerCase(Locale.ROOT),
                normalize(request.companyName()), title, details, request.category(), request.urgency(),
                classification, workItem.getId()));
        events.publishEvent(new NewPublicRequestEvent(organization.getId(), organization.getSlug(),
                organization.getName(), submission.getId(), submission.getReferenceNumber(), submission.getRequesterName(),
                submission.getRequesterEmail(), submission.getCompanyName(), submission.getTitle(),
                submission.getCategory(), submission.getSuggestedPriority()));
        usageEvents.recordPublicIntakeClassificationSafely(organization.getId(), organization.getSlug(),
                submission.getId());
        return receipt(submission, false);
    }

    @Transactional(readOnly = true)
    public List<RequestSubmission> currentRequests() {
        return submissions.findAllByTenantIdOrderByCreatedAtDesc(tenantContext.tenantId());
    }

    @Transactional(readOnly = true)
    public RequestSubmission currentRequest(UUID id) {
        return submissions.findByIdAndTenantId(id, tenantContext.tenantId())
                .orElseThrow(() -> new SaasException(HttpStatus.NOT_FOUND, "Request was not found"));
    }

    private void assertNotBot(IntakeRequest request) {
        if (properties.isHoneypotEnabled() && request.website() != null && !request.website().isBlank()) {
            throw new SaasException(HttpStatus.BAD_REQUEST, "Request could not be processed");
        }
    }

    private TenantOrganization activeOrganization(String value, String portalToken) {
        String slug = value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
        if (!slug.matches("[a-z0-9-]{3,80}")) throw portalNotFound();
        TenantOrganization organization = organizations.findBySlug(slug)
                .filter(candidate -> "ACTIVE".equals(candidate.getStatus()))
                .orElseThrow(this::portalNotFound);
        if (organization.portalTokenRequired()
                && !tokenHasher.matches(portalToken, organization.getPortalTokenHash())) {
            throw portalNotFound();
        }
        return organization;
    }

    private SaasException portalNotFound() {
        return new SaasException(HttpStatus.NOT_FOUND, "Request portal was not found");
    }

    private String normalizeKey(String value) {
        String key = value == null || value.isBlank() ? UUID.randomUUID().toString() : value.trim();
        if (!key.matches("[A-Za-z0-9._:-]{8,80}")) {
            throw new SaasException(HttpStatus.BAD_REQUEST, "A valid Idempotency-Key is required");
        }
        return key;
    }

    private String normalize(String value) { return value.trim().replaceAll("\\s+", " "); }

    private Receipt receipt(RequestSubmission submission, boolean replayed) {
        return new Receipt(submission.getId(), submission.getReferenceNumber(), submission.getStatus(),
                submission.getCategory(), submission.getSuggestedPriority(),
                submission.getRecommendedNextAction(), submission.getCreatedAt(), replayed);
    }

    public record IntakeRequest(
            @NotBlank @Size(max = 120) String requesterName,
            @NotBlank @Email @Size(max = 254) String requesterEmail,
            @NotBlank @Size(max = 160) String companyName,
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(min = 10, max = 2000) String details,
            RequestCategory category,
            RequestUrgency urgency,
            @Size(max = 200) String website) {}

    public record Portal(String organizationName, String organizationSlug, boolean portalTokenRequired) {}

    public record Receipt(UUID requestId, String referenceNumber, String status, RequestCategory category,
            com.fromzerotohero.mission.workitem.Priority suggestedPriority,
            String recommendedNextAction, java.time.Instant receivedAt, boolean replayed) {}
}
