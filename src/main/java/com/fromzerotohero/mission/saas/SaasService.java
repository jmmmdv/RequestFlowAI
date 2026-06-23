package com.fromzerotohero.mission.saas;

import com.fromzerotohero.mission.security.TenantContext;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import com.fromzerotohero.mission.intake.PortalTokenHasher;

@Service
public class SaasService {
    private final TenantOrganizationRepository organizations;
    private final TenantMembershipRepository memberships;
    private final TenantInvitationRepository invitations;
    private final BillingSubscriptionRepository subscriptions;
    private final TenantContext tenantContext;
    private final QuotaService quotas;
    private final PortalTokenHasher tokenHasher;
    private final String frontendUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public SaasService(TenantOrganizationRepository organizations, TenantMembershipRepository memberships,
            TenantInvitationRepository invitations, BillingSubscriptionRepository subscriptions,
            TenantContext tenantContext, QuotaService quotas, PortalTokenHasher tokenHasher,
            @Value("${mission.frontend-url:http://localhost:8080}") String frontendUrl) {
        this.organizations = organizations; this.memberships = memberships;
        this.invitations = invitations; this.subscriptions = subscriptions;
        this.tenantContext = tenantContext; this.quotas = quotas;
        this.tokenHasher = tokenHasher;
        this.frontendUrl = frontendUrl.replaceAll("/$", "");
    }

    @Transactional
    public OrganizationOverview current() {
        TenantOrganization organization = ensureCurrentOrganization();
        TenantMembership membership = ensureCurrentMembership(organization.getId());
        BillingSubscription subscription = subscriptions.findById(organization.getId())
                .orElseGet(() -> subscriptions.save(new BillingSubscription(organization.getId())));
        return overview(organization, membership, subscription);
    }

    @Transactional
    public OrganizationOverview rename(UpdateOrganizationRequest request) {
        TenantOrganization organization = ensureCurrentOrganization();
        String slug = normalizeSlug(request.slug());
        organizations.findBySlug(slug)
                .filter(candidate -> !candidate.getId().equals(organization.getId()))
                .ifPresent(candidate -> { throw new SaasException(HttpStatus.CONFLICT, "Organization slug is already used"); });
        organization.rename(normalizeName(request.name()), slug);
        return current();
    }

    @Transactional
    public InvitationCreated invite(InviteMemberRequest request) {
        ensureCurrentOrganization();
        byte[] bytes = new byte[32]; secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        TenantInvitation invitation = invitations.save(new TenantInvitation(tenantContext.tenantId(),
                request.email().trim().toLowerCase(Locale.ROOT), request.role(), hash(token),
                tenantContext.userId(), Instant.now().plus(Duration.ofDays(7))));
        return new InvitationCreated(invitation.getId(), invitation.getEmail(), invitation.getRole(),
                invitation.getExpiresAt(), token);
    }

    @Transactional
    public OrganizationOverview accept(AcceptInvitationRequest request) {
        TenantInvitation invitation = invitations.findByTokenHash(hash(request.token()))
                .orElseThrow(() -> new SaasException(HttpStatus.NOT_FOUND, "Invitation was not found"));
        if (!invitation.isUsable(Instant.now())) {
            throw new SaasException(HttpStatus.GONE, "Invitation has expired or was already accepted");
        }
        String email = tenantContext.requireEmail();
        if (!email.equalsIgnoreCase(invitation.getEmail())) {
            throw new SaasException(HttpStatus.FORBIDDEN, "Invitation belongs to a different email address");
        }
        memberships.findByTenantIdAndUserId(invitation.getTenantId(), tenantContext.userId())
                .orElseGet(() -> memberships.save(new TenantMembership(invitation.getTenantId(),
                        tenantContext.userId(), email, invitation.getRole())));
        invitation.accept();
        TenantOrganization organization = organizations.findById(invitation.getTenantId()).orElseThrow();
        BillingSubscription subscription = subscriptions.findById(organization.getId())
                .orElseGet(() -> subscriptions.save(new BillingSubscription(organization.getId())));
        TenantMembership membership = memberships.findByTenantIdAndUserId(organization.getId(), tenantContext.userId()).orElseThrow();
        return overview(organization, membership, subscription);
    }

    @Transactional(readOnly = true)
    public List<TenantMembership> members() {
        return memberships.findAllByTenantIdOrderByJoinedAt(tenantContext.tenantId());
    }

    @Transactional(readOnly = true)
    public List<TenantInvitation> invitations() {
        return invitations.findAllByTenantIdOrderByCreatedAtDesc(tenantContext.tenantId());
    }

    private TenantOrganization ensureCurrentOrganization() {
        UUID tenantId = tenantContext.tenantId();
        return organizations.findById(tenantId).orElseGet(() -> {
            String name = normalizeName(tenantContext.organizationName());
            String base = normalizeSlug(name);
            String slug = organizations.existsBySlug(base) ? base + "-" + tenantId.toString().substring(0, 8) : base;
            return organizations.save(new TenantOrganization(tenantId, name, slug));
        });
    }

    private TenantMembership ensureCurrentMembership(UUID tenantId) {
        return memberships.findByTenantIdAndUserId(tenantId, tenantContext.userId())
                .orElseGet(() -> memberships.save(new TenantMembership(tenantId, tenantContext.userId(),
                        tenantContext.email(), tenantContext.currentRole())));
    }

    @Transactional
    public OrganizationOverview completeOnboarding(UpdateOrganizationRequest request) {
        TenantOrganization organization = ensureCurrentOrganization();
        if (organization.isOnboardingCompleted()) {
            throw new SaasException(HttpStatus.CONFLICT, "Onboarding is already complete");
        }
        String slug = normalizeSlug(request.slug());
        organizations.findBySlug(slug)
                .filter(candidate -> !candidate.getId().equals(organization.getId()))
                .ifPresent(candidate -> { throw new SaasException(HttpStatus.CONFLICT, "Organization slug is already used"); });
        organization.rename(normalizeName(request.name()), slug);
        organization.completeOnboarding();
        organizations.save(organization);
        TenantMembership membership = ensureCurrentMembership(organization.getId());
        BillingSubscription subscription = subscriptions.findById(organization.getId())
                .orElseGet(() -> subscriptions.save(new BillingSubscription(organization.getId())));
        return overview(organization, membership, subscription);
    }

    @Transactional(readOnly = true)
    public PortalSettings portalSettings() {
        TenantOrganization organization = ensureCurrentOrganization();
        return portalSettings(organization, null);
    }

    @Transactional
    public PortalSettings updatePortal(UpdatePortalRequest request) {
        ensureCurrentOrganization();
        TenantOrganization organization = organizations.findById(tenantContext.tenantId()).orElseThrow();
        organization.updateRetentionDays(request.requestRetentionDays());
        if (request.clearPortalToken()) organization.clearPortalToken();
        organizations.save(organization);
        return portalSettings(organization, null);
    }

    @Transactional
    public PortalTokenRotated rotatePortalToken() {
        TenantOrganization organization = ensureCurrentOrganization();
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        organization.rotatePortalToken(tokenHasher.hash(token));
        organizations.save(organization);
        return new PortalTokenRotated(token, portalSettings(organization, token).shareableFormUrl());
    }

    private PortalSettings portalSettings(TenantOrganization organization, String portalToken) {
        String path = publicFormPath(organization.getSlug(), portalToken);
        return new PortalSettings(organization.getSlug(), organization.getName(), organization.portalTokenRequired(),
                organization.getRequestRetentionDays(), path, frontendUrl + path);
    }

    private String publicFormPath(String slug, String portalToken) {
        String path = "/public-request.html?organization=" + slug;
        if (portalToken != null && !portalToken.isBlank()) {
            path += "&token=" + java.net.URLEncoder.encode(portalToken, StandardCharsets.UTF_8);
        }
        return path;
    }

    private OrganizationOverview overview(TenantOrganization organization, TenantMembership membership,
            BillingSubscription subscription) {
        return new OrganizationOverview(organization.getId(), organization.getName(), organization.getSlug(),
                organization.getPlan(), organization.getStatus(), membership.getRole(), subscription.getStripeCustomerId() != null,
                subscription.getStatus(), quotas.usage(organization.getId()), organization.isOnboardingCompleted(),
                publicFormPath(organization.getSlug(), null));
    }

    private String normalizeName(String value) { return value.trim().replaceAll("\\s+", " "); }
    private String normalizeSlug(String value) {
        String slug = value.toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.length() < 3) throw new SaasException(HttpStatus.BAD_REQUEST, "Organization slug must contain at least 3 characters");
        return slug.substring(0, Math.min(80, slug.length()));
    }
    private String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    public record UpdateOrganizationRequest(@NotBlank @Size(max = 120) String name,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{3,80}") String slug) {}
    public record InviteMemberRequest(@NotBlank @Email @Size(max = 254) String email,
            @NotNull MembershipRole role) {}
    public record AcceptInvitationRequest(@NotBlank @Size(min = 32, max = 100) String token) {}
    public record InvitationCreated(UUID invitationId, String email, MembershipRole role,
            Instant expiresAt, String token) {}
    public record OrganizationOverview(UUID id, String name, String slug, Plan plan, String status,
            MembershipRole currentUserRole, boolean billingConfigured, String subscriptionStatus,
            QuotaService.UsageSnapshot usage, boolean onboardingCompleted, String publicFormPath) {}
    public record PortalSettings(String organizationSlug, String organizationName, boolean portalTokenRequired,
            int requestRetentionDays, String publicFormPath, String shareableFormUrl) {}
    public record PortalTokenRotated(String portalToken, String shareableFormUrl) {}
    public record UpdatePortalRequest(
            @jakarta.validation.constraints.Min(7) @jakarta.validation.constraints.Max(2555) int requestRetentionDays,
            boolean clearPortalToken) {}
}
