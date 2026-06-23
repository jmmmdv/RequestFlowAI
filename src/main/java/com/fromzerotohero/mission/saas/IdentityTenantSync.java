package com.fromzerotohero.mission.saas;

import java.util.UUID;

/**
 * Transfers a Cognito user's tenant claims after they accept a team invitation.
 * No-op in local development; production uses Admin APIs when a user pool is configured.
 */
public interface IdentityTenantSync {

    /**
     * @return true when identity provider attributes were updated and the user should sign in again
     */
    boolean syncInvitedMember(String email, UUID tenantId, String organizationName, MembershipRole role);
}
