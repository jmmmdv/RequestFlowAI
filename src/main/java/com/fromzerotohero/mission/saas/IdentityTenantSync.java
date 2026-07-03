package com.fromzerotohero.mission.saas;

import java.util.UUID;

/**
 * Optional hook for external identity providers after a team invitation is accepted.
 * The lean Vercel + Neon MVP uses the no-op implementation.
 */
public interface IdentityTenantSync {

    /**
     * @return true when identity provider attributes were updated and the user should sign in again
     */
    boolean syncInvitedMember(String email, UUID tenantId, String organizationName, MembershipRole role);
}
