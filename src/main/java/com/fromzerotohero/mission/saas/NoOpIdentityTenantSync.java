package com.fromzerotohero.mission.saas;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NoOpIdentityTenantSync implements IdentityTenantSync {
    @Override
    public boolean syncInvitedMember(String email, UUID tenantId, String organizationName, MembershipRole role) {
        return false;
    }
}
