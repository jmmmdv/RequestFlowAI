package com.fromzerotohero.mission.intake;

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

    public RequestIntakeService(TenantOrganizationRepository organizations,
            RequestSubmissionRepository submissions, WorkItemRepository workItems,
            RuleBasedRequestClassifier classifier, QuotaService quotas, TenantContext tenantContext) {
        this.organizations = organizations;
        this.submissions = submissions;
        this.workItems = workItems;
        this.classifier = classifier;
        this.quotas = quotas;
        this.tenantContext = tenantContext;
    }

    @Transactional(readOnly = true)
    public Portal portal(String organizationSlug) {
        TenantOrganization organization = activeOrganization(organizationSlug);
        return new Portal(organization.getName(), organization.getSlug());
    }

    @Transactional
    public Receipt submit(String organizationSlug, IntakeRequest request, String requestedKey) {
        TenantOrganization organization = activeOrganization(organizationSlug);
        String idempotencyKey = normalizeKey(requestedKey);
        var existing = submissions.findByTenantIdAndIdempotencyKey(organization.getId(), idempotencyKey);
        if (existing.isPresent()) return receipt(existing.get(), true);

        String title = normalize(request.title());
        String details = normalize(request.details());
        var classification = classifier.classify(title, details);
        quotas.assertWorkItemCapacity(organization.getId(), 1);
        WorkItem workItem = workItems.save(new WorkItem(title, classification.internalSummary(),
                classification.priority(), WorkStatus.BACKLOG, organization.getId()));
        RequestSubmission submission = submissions.save(new RequestSubmission(organization.getId(),
                idempotencyKey, normalize(request.requesterName()), request.requesterEmail().trim().toLowerCase(Locale.ROOT),
                title, details, classification, workItem.getId()));
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

    private TenantOrganization activeOrganization(String value) {
        String slug = value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
        if (!slug.matches("[a-z0-9-]{3,80}")) throw portalNotFound();
        return organizations.findBySlug(slug).filter(organization -> "ACTIVE".equals(organization.getStatus()))
                .orElseThrow(this::portalNotFound);
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
        return new Receipt(submission.getId(), submission.getCategory(), submission.getSuggestedPriority(),
                submission.getRecommendedNextAction(), submission.getCreatedAt(), replayed);
    }

    public record IntakeRequest(
            @NotBlank @Size(max = 120) String requesterName,
            @NotBlank @Email @Size(max = 254) String requesterEmail,
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(min = 10, max = 2000) String details) {}

    public record Portal(String organizationName, String organizationSlug) {}

    public record Receipt(UUID requestId, RequestCategory category,
            com.fromzerotohero.mission.workitem.Priority suggestedPriority,
            String recommendedNextAction, java.time.Instant receivedAt, boolean replayed) {}
}
