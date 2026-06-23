package com.fromzerotohero.mission.saas;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRemoveUserFromGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUserGlobalSignOutRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;

@Component
@ConditionalOnExpression("!'${mission.cognito.user-pool-id:}'.isBlank()")
public class CognitoIdentityTenantSync implements IdentityTenantSync {
    private static final List<MembershipRole> ALL_ROLES =
            List.of(MembershipRole.ADMIN, MembershipRole.MEMBER, MembershipRole.VIEWER);

    private final CognitoIdentityProviderClient cognito;
    private final String userPoolId;

    public CognitoIdentityTenantSync(CognitoIdentityProviderClient cognito,
            @Value("${mission.cognito.user-pool-id}") String userPoolId) {
        this.cognito = cognito;
        this.userPoolId = userPoolId;
    }

    @Override
    public boolean syncInvitedMember(String email, UUID tenantId, String organizationName, MembershipRole role) {
        String username = email.trim().toLowerCase();
        try {
            cognito.adminUpdateUserAttributes(AdminUpdateUserAttributesRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .userAttributes(
                            AttributeType.builder().name("custom:tenant_id").value(tenantId.toString()).build(),
                            AttributeType.builder().name("custom:organization_name").value(organizationName).build())
                    .build());
            for (MembershipRole existing : ALL_ROLES) {
                if (existing == role) continue;
                try {
                    cognito.adminRemoveUserFromGroup(AdminRemoveUserFromGroupRequest.builder()
                            .userPoolId(userPoolId)
                            .username(username)
                            .groupName(existing.name())
                            .build());
                } catch (Exception ignored) {
                    // User may not belong to every group.
                }
            }
            cognito.adminAddUserToGroup(AdminAddUserToGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .groupName(role.name())
                    .build());
            cognito.adminUserGlobalSignOut(AdminUserGlobalSignOutRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .build());
            return true;
        } catch (Exception exception) {
            throw new SaasException(HttpStatus.BAD_GATEWAY,
                    "Invitation was accepted but Cognito identity could not be synchronized");
        }
    }
}
