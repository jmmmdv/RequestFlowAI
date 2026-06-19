package com.fromzerotohero.mission.intake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fromzerotohero.mission.agent.AgentRunRepository;
import com.fromzerotohero.mission.saas.Plan;
import com.fromzerotohero.mission.saas.TenantOrganizationRepository;
import com.fromzerotohero.mission.workitem.WorkItemRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "mission.seed.enabled=false")
@AutoConfigureMockMvc
class PublicRequestIntakeIntegrationTest {
    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired MockMvc mvc;
    @Autowired RequestSubmissionRepository submissions;
    @Autowired WorkItemRepository workItems;
    @Autowired AgentRunRepository agentRuns;
    @Autowired TenantOrganizationRepository organizations;

    @BeforeEach
    void cleanState() {
        submissions.deleteAll();
        agentRuns.deleteAll();
        workItems.deleteAll();
        var organization = organizations.findById(TENANT).orElseThrow();
        organization.rename("Local Development", "local");
        organization.changePlan(Plan.FREE);
        organizations.save(organization);
    }

    @Test
    void publicSubmissionCreatesOriginalRequestAndTenantOwnedWork() throws Exception {
        mvc.perform(get("/api/public/intake/local"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName").value("Local Development"));

        mvc.perform(post("/api/public/intake/local")
                        .header("Idempotency-Key", "public-request-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requesterName":"Taylor Client",
                                  "requesterEmail":"Taylor@Example.com",
                                  "title":"Booking form is down",
                                  "details":"Urgent: customers cannot access the booking form today."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/requests/.+")))
                .andExpect(jsonPath("$.category").value("SUPPORT"))
                .andExpect(jsonPath("$.suggestedPriority").value("CRITICAL"))
                .andExpect(jsonPath("$.recommendedNextAction").isNotEmpty())
                .andExpect(jsonPath("$.internalSummary").doesNotExist())
                .andExpect(jsonPath("$.workItemId").doesNotExist())
                .andExpect(jsonPath("$.replayed").value(false));

        assertThat(submissions.findAllByTenantIdOrderByCreatedAtDesc(TENANT)).singleElement()
                .satisfies(request -> {
                    assertThat(request.getRequesterEmail()).isEqualTo("taylor@example.com");
                    assertThat(request.getTenantId()).isEqualTo(TENANT);
                    assertThat(request.getWorkItemId()).isNotNull();
                });
        assertThat(workItems.findAllByTenantIdOrderByUpdatedAtDesc(TENANT)).singleElement()
                .satisfies(item -> assertThat(item.getTitle()).isEqualTo("Booking form is down"));
    }

    @Test
    void idempotencyReplayDoesNotDuplicateRequestOrWorkItem() throws Exception {
        String body = """
                {"requesterName":"Jamie","requesterEmail":"jamie@example.com",
                 "title":"Please update our logo","details":"Please update the website logo before next week."}
                """;
        mvc.perform(post("/api/public/intake/local").header("Idempotency-Key", "same-request-001")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/public/intake/local").header("Idempotency-Key", "same-request-001")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.replayed").value(true));

        assertThat(submissions.count()).isOne();
        assertThat(workItems.countByTenantId(TENANT)).isOne();
    }

    @Test
    void authenticatedWorkspaceCanListOriginalRequests() throws Exception {
        mvc.perform(post("/api/public/intake/local").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requesterName":"Avery","requesterEmail":"avery@example.com",
                                 "title":"Invoice question","details":"Please explain the latest invoice charge."}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requesterName").value("Avery"))
                .andExpect(jsonPath("$[0].category").value("BILLING"));
    }

    @Test
    void hidesUnknownPortalsAndRejectsInvalidInput() throws Exception {
        mvc.perform(get("/api/public/intake/not-a-customer"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Request portal was not found"));
        mvc.perform(post("/api/public/intake/local").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requesterName\":\"\",\"requesterEmail\":\"bad\",\"title\":\"\",\"details\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }
}
