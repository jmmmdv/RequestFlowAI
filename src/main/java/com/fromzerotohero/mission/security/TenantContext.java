package com.fromzerotohero.mission.security;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

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

    private Jwt authenticatedJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("An authenticated JWT is required");
        }
        return jwt;
    }
}
