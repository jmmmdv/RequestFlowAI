package com.fromzerotohero.mission.security;

import com.fromzerotohero.mission.saas.MembershipRole;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class TenantContext {
    private final boolean securityEnabled;
    private final UUID defaultTenantId;

    public TenantContext(@Value("${mission.security.enabled:false}") boolean securityEnabled,
            @Value("${mission.tenant.default-id}") UUID defaultTenantId) {
        this.securityEnabled = securityEnabled;
        this.defaultTenantId = defaultTenantId;
    }

    public UUID tenantId() {
        if (!securityEnabled) return defaultTenantId;
        Jwt jwt = authenticatedJwt();
        String claim = jwt.getClaimAsString("tenant_id");
        try {
            return UUID.fromString(claim);
        } catch (RuntimeException exception) {
            throw new AccessDeniedException("A valid tenant_id claim is required");
        }
    }

    public String userId() {
        if (!securityEnabled) return "local-developer";
        String subject = authenticatedJwt().getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AccessDeniedException("A non-empty subject claim is required");
        }
        return subject;
    }

    public String email() {
        if (!securityEnabled) return "developer@local.test";
        String email = authenticatedJwt().getClaimAsString("email");
        return email == null || email.isBlank() ? null : email.trim().toLowerCase();
    }

    public String requireEmail() {
        String email = email();
        if (!securityEnabled) return email;
        if (email == null) {
            throw new AccessDeniedException("A valid email claim is required");
        }
        return email;
    }

    public String organizationName() {
        if (!securityEnabled) return "Local Development";
        String name = authenticatedJwt().getClaimAsString("organization_name");
        if (name == null) name = authenticatedJwt().getClaimAsString("custom:organization_name");
        return name == null || name.isBlank() ? "My Organization" : name.trim();
    }

    public MembershipRole currentRole() {
        if (!securityEnabled) return MembershipRole.ADMIN;
        List<String> roles = authenticatedJwt().getClaimAsStringList("roles");
        List<String> groups = authenticatedJwt().getClaimAsStringList("cognito:groups");
        if (contains(roles, "ADMIN") || contains(groups, "ADMIN")) return MembershipRole.ADMIN;
        if (contains(roles, "MEMBER") || contains(groups, "MEMBER")) return MembershipRole.MEMBER;
        return MembershipRole.VIEWER;
    }

    private boolean contains(List<String> values, String expected) {
        return values != null && values.stream().anyMatch(expected::equalsIgnoreCase);
    }

    private Jwt authenticatedJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("An authenticated JWT is required");
        }
        return jwt;
    }
}
