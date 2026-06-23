package com.fromzerotohero.mission.intake;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fromzerotohero.mission.agent.AgentRunRepository;
import com.fromzerotohero.mission.saas.MembershipRole;
import com.fromzerotohero.mission.saas.Plan;
import com.fromzerotohero.mission.saas.TenantMembership;
import com.fromzerotohero.mission.saas.TenantMembershipRepository;
import com.fromzerotohero.mission.saas.TenantOrganizationRepository;
import com.fromzerotohero.mission.workitem.WorkItemRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "mission.seed.enabled=false")
@AutoConfigureMockMvc
class PublicRequestIntakeNotificationIntegrationTest {
    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired MockMvc mvc;
    @Autowired RequestSubmissionRepository submissions;
    @Autowired WorkItemRepository workItems;
    @Autowired AgentRunRepository agentRuns;
    @Autowired TenantOrganizationRepository organizations;
    @Autowired TenantMembershipRepository memberships;

    @MockBean IntakeNotificationSender notificationSender;

    @BeforeEach
    void cleanState() {
        submissions.deleteAll();
        agentRuns.deleteAll();
        workItems.deleteAll();
        memberships.deleteAll();
        memberships.save(new TenantMembership(TENANT, "admin-1", "owner@example.com", MembershipRole.ADMIN));
        var organization = organizations.findById(TENANT).orElseThrow();
        organization.rename("Local Development", "local");
        organization.changePlan(Plan.FREE);
        organization.clearPortalToken();
        organizations.save(organization);
    }

    @Test
    void successfulSubmissionNotifiesAdminsAfterCommit() throws Exception {
        mvc.perform(post("/api/public/intake/local")
                        .header("Idempotency-Key", "notify-request-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requesterName":"Taylor Client",
                                  "requesterEmail":"taylor@example.com",
                                  "companyName":"Taylor Studio",
                                  "title":"Booking form is down",
                                  "details":"Customers cannot access the booking form today.",
                                  "category":"SUPPORT",
                                  "urgency":"URGENT"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(notificationSender, timeout(2000)).send(
                argThat(recipients -> recipients.contains("owner@example.com")),
                argThat(subject -> subject.contains("Taylor Client") && subject.contains("Booking form is down")),
                argThat(body -> body.contains("taylor@example.com")
                        && body.contains("Category: support")
                        && body.contains("Suggested priority: CRITICAL")
                        && body.contains("#workspace")));
    }

    @Test
    void idempotentReplayDoesNotSendAnotherNotification() throws Exception {
        String body = """
                {"requesterName":"Jamie","requesterEmail":"jamie@example.com","companyName":"Northwind",
                 "title":"Please update our logo","details":"Please update the website logo before next week."}
                """;
        mvc.perform(post("/api/public/intake/local").header("Idempotency-Key", "notify-request-002")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/public/intake/local").header("Idempotency-Key", "notify-request-002")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        verify(notificationSender, timeout(2000).times(1))
                .send(anyList(), anyString(), anyString());
    }

    @Test
    void submissionSucceedsWhenNotificationSenderFails() throws Exception {
        org.mockito.Mockito.doThrow(new RuntimeException("smtp down"))
                .when(notificationSender).send(anyList(), anyString(), anyString());

        mvc.perform(post("/api/public/intake/local")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requesterName":"Avery","requesterEmail":"avery@example.com","companyName":"Avery & Co",
                                 "title":"Invoice question","details":"Please explain the latest invoice charge."}
                                """))
                .andExpect(status().isCreated());

        verify(notificationSender, timeout(2000)).send(anyList(), anyString(), anyString());
        org.assertj.core.api.Assertions.assertThat(submissions.count()).isOne();
    }
}
