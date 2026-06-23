package com.fromzerotohero.mission.intake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fromzerotohero.mission.saas.MembershipRole;
import com.fromzerotohero.mission.saas.TenantMembership;
import com.fromzerotohero.mission.saas.TenantMembershipRepository;
import com.fromzerotohero.mission.workitem.Priority;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntakeNotificationServiceTest {
    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock private IntakeNotificationProperties properties;
    @Mock private TenantMembershipRepository memberships;
    @Mock private IntakeNotificationSender sender;

    private IntakeNotificationService service;

    @BeforeEach
    void setUp() {
        service = new IntakeNotificationService(properties, memberships, sender, "https://app.example.com");
        when(properties.isEnabled()).thenReturn(true);
    }

    @Test
    void sendsNotificationToAdminRecipients() {
        when(memberships.findAllByTenantIdAndRoleOrderByJoinedAtAsc(TENANT, MembershipRole.ADMIN))
                .thenReturn(List.of(new TenantMembership(TENANT, "admin-1", "Owner@Example.com", MembershipRole.ADMIN)));

        service.notifyNewRequest(sampleNotification());

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq(List.of("owner@example.com")), contains("Taylor Client"), body.capture());
        assertThat(body.getValue())
                .contains("Taylor Client")
                .contains("taylor@example.com")
                .contains("Booking form is down")
                .contains("Category: support")
                .contains("Suggested priority: CRITICAL")
                .contains("Reference: RF-12345678")
                .contains("https://app.example.com/#workspace");
    }

    @Test
    void usesFallbackRecipientWhenNoAdminsExist() {
        when(memberships.findAllByTenantIdAndRoleOrderByJoinedAtAsc(TENANT, MembershipRole.ADMIN))
                .thenReturn(List.of());
        when(properties.getFallbackRecipient()).thenReturn("fallback@example.com");

        service.notifyNewRequest(sampleNotification());

        verify(sender).send(eq(List.of("fallback@example.com")), anyString(), anyString());
    }

    @Test
    void skipsWhenNotificationsDisabled() {
        when(properties.isEnabled()).thenReturn(false);

        service.notifyNewRequest(sampleNotification());

        verify(sender, never()).send(anyList(), anyString(), anyString());
    }

    @Test
    void doesNotPropagateSenderFailures() {
        when(memberships.findAllByTenantIdAndRoleOrderByJoinedAtAsc(TENANT, MembershipRole.ADMIN))
                .thenReturn(List.of(new TenantMembership(TENANT, "admin-1", "owner@example.com", MembershipRole.ADMIN)));
        doThrow(new RuntimeException("smtp unavailable")).when(sender)
                .send(anyList(), anyString(), anyString());

        service.notifyNewRequest(sampleNotification());
    }

    private NewPublicRequestNotification sampleNotification() {
        return new NewPublicRequestNotification(TENANT, "Local Development", UUID.randomUUID(), "RF-12345678",
                "Taylor Client", "taylor@example.com", "Taylor Studio", "Booking form is down",
                RequestCategory.SUPPORT, Priority.CRITICAL, "https://app.example.com/#workspace");
    }
}
