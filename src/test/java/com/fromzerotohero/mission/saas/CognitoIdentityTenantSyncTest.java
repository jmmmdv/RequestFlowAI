package com.fromzerotohero.mission.saas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;

class CognitoIdentityTenantSyncTest {
    private CognitoIdentityProviderClient cognito;
    private CognitoIdentityTenantSync sync;

    @BeforeEach
    void setUp() {
        cognito = mock(CognitoIdentityProviderClient.class);
        sync = new CognitoIdentityTenantSync(cognito, "us-east-1_example");
    }

    @Test
    void syncInvitedMemberUpdatesTenantClaimsAndGroup() {
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        assertThat(sync.syncInvitedMember("invited@example.com", tenantId, "Brightside", MembershipRole.VIEWER))
                .isTrue();

        ArgumentCaptor<AdminUpdateUserAttributesRequest> update =
                ArgumentCaptor.forClass(AdminUpdateUserAttributesRequest.class);
        verify(cognito).adminUpdateUserAttributes(update.capture());
        assertThat(update.getValue().username()).isEqualTo("invited@example.com");
        assertThat(update.getValue().userAttributes()).anyMatch(attribute ->
                attribute.name().equals("custom:tenant_id") && attribute.value().equals(tenantId.toString()));

        ArgumentCaptor<AdminAddUserToGroupRequest> group = ArgumentCaptor.forClass(AdminAddUserToGroupRequest.class);
        verify(cognito).adminAddUserToGroup(group.capture());
        assertThat(group.getValue().groupName()).isEqualTo("VIEWER");
    }

    @Test
    void syncInvitedMemberSurfacesCognitoFailures() {
        when(cognito.adminUpdateUserAttributes(any(AdminUpdateUserAttributesRequest.class)))
                .thenThrow(new RuntimeException("Cognito unavailable"));

        assertThatThrownBy(() -> sync.syncInvitedMember("invited@example.com",
                UUID.fromString("00000000-0000-0000-0000-000000000099"), "Brightside", MembershipRole.MEMBER))
                .isInstanceOf(SaasException.class)
                .hasMessageContaining("Cognito identity could not be synchronized");
    }
}
