package com.fromzerotohero.mission.security;

import com.fromzerotohero.mission.saas.MembershipRole;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class TenantContext {
    private final boolean securityEnabled;
    private final UUID defaultTenantId;
    private final String defaultUserEmail;
    private final String defaultOrganizationName;

    public TenantContext(@Value("${mission.security.enabled:false}") boolean securityEnabled,
            @Value("${mission.tenant.default-id}") UUID defaultTenantId,
            @Value("${requestflow.admin.email:developer@local.test}") String defaultUserEmail,
            @Value("${requestflow.admin.organization-name:Local Development}") String defaultOrganizationName) {
        this.securityEnabled = securityEnabled;
        this.defaultTenantId = defaultTenantId;
        this.defaultUserEmail = normalizeEmail(defaultUserEmail);
        this.defaultOrganizationName = defaultOrganizationName == null || defaultOrganizationName.isBlank()
                ? "Local Development" : defaultOrganizationName.trim();
    }

    public UUID tenantId() {
        if (!securityEnabled) return defaultTenantId;
        if (!(authentication().getPrincipal() instanceof Jwt)) return defaultTenantId;
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
        Authentication authentication = authentication();
        if (!(authentication.getPrincipal() instanceof Jwt)) return authentication.getName();
        String subject = authenticatedJwt().getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AccessDeniedException("A non-empty subject claim is required");
        }
        return subject;
    }

    public String email() {
        if (!securityEnabled) return "developer@local.test";
        if (!(authentication().getPrincipal() instanceof Jwt)) return defaultUserEmail;
        String email = authenticatedJwt().getClaimAsString("email");
        return normalizeEmail(email);
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
        if (!(authentication().getPrincipal() instanceof Jwt)) return defaultOrganizationName;
        String name = authenticatedJwt().getClaimAsString("organization_name");
        if (name == null) name = authenticatedJwt().getClaimAsString("custom:organization_name");
        return name == null || name.isBlank() ? "My Organization" : name.trim();
    }

    public MembershipRole currentRole() {
        if (!securityEnabled) return MembershipRole.ADMIN;
        Authentication authentication = authentication();
        if (!(authentication.getPrincipal() instanceof Jwt)) {
            if (hasAuthority(authentication, "ROLE_ADMIN")) return MembershipRole.ADMIN;
            if (hasAuthority(authentication, "ROLE_MEMBER")) return MembershipRole.MEMBER;
            return MembershipRole.VIEWER;
        }
        List<String> roles = authenticatedJwt().getClaimAsStringList("roles");
        List<String> groups = authenticatedJwt().getClaimAsStringList("cognito:groups");
        if (contains(roles, "ADMIN") || contains(groups, "ADMIN")) return MembershipRole.ADMIN;
        if (contains(roles, "MEMBER") || contains(groups, "MEMBER")) return MembershipRole.MEMBER;
        return MembershipRole.VIEWER;
    }

    private boolean contains(List<String> values, String expected) {
        return values != null && values.stream().anyMatch(expected::equalsIgnoreCase);
    }

    private boolean hasAuthority(Authentication authentication, String expected) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(expected::equals);
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase();
    }

    private Authentication authentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AccessDeniedException("An authenticated user is required");
        }
        return authentication;
    }

    private Jwt authenticatedJwt() {
        Authentication authentication = authentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("An authenticated JWT is required");
        }
        return jwt;
    }
}
