package com.fromzerotohero.mission.saas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fromzerotohero.mission.intake.RequestRetentionService;
import com.fromzerotohero.mission.intake.RequestSubmissionRepository;
import com.fromzerotohero.mission.workitem.WorkItemRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "mission.seed.enabled=false")
@AutoConfigureMockMvc
class PilotCompletenessIntegrationTest {
    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired MockMvc mvc;
    @Autowired TenantOrganizationRepository organizations;
    @Autowired RequestSubmissionRepository submissions;
    @Autowired WorkItemRepository workItems;
    @Autowired RequestRetentionService retentionService;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void resetOrganization() {
        submissions.deleteAll();
        workItems.deleteAll();
        var organization = organizations.findById(TENANT).orElseThrow();
        organization.rename("Local Development", "local");
        organization.changePlan(Plan.FREE);
        organization.clearPortalToken();
        organization.updateRetentionDays(365);
        organization.completeOnboarding();
        organizations.save(organization);
        jdbc.update("update tenant set onboarding_completed = true where id = ?", TENANT);
    }

    @Test
    void completesStartFreeOnboarding() throws Exception {
        jdbc.update("update tenant set onboarding_completed = false where id = ?", TENANT);

        mvc.perform(get("/api/saas/organization"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingCompleted").value(false));

        mvc.perform(post("/api/saas/onboarding").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Brightside Services\",\"slug\":\"brightside-services\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Brightside Services"))
                .andExpect(jsonPath("$.slug").value("brightside-services"))
                .andExpect(jsonPath("$.onboardingCompleted").value(true))
                .andExpect(jsonPath("$.publicFormPath").value("/public-request.html?organization=brightside-services"));

        jdbc.update("update tenant set slug = 'local', name = 'Local Development' where id = ?", TENANT);
    }

    @Test
    void adminCanManagePortalSettingsAndRotateToken() throws Exception {
        mvc.perform(get("/api/saas/portal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestRetentionDays").value(365))
                .andExpect(jsonPath("$.portalTokenRequired").value(false));

        mvc.perform(patch("/api/saas/portal").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestRetentionDays\":90,\"clearPortalToken\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestRetentionDays").value(90));

        mvc.perform(post("/api/saas/portal/rotate-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareableFormUrl").isNotEmpty())
                .andExpect(jsonPath("$.portalToken").isNotEmpty());

        mvc.perform(get("/api/saas/portal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portalTokenRequired").value(true));

        mvc.perform(patch("/api/saas/portal").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestRetentionDays\":90,\"clearPortalToken\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portalTokenRequired").value(false));
    }

    @Test
    void retentionServicePurgesExpiredSubmissions() throws Exception {
        jdbc.update("update tenant set request_retention_days = 30 where id = ?", TENANT);

        mvc.perform(post("/api/public/intake/local").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requesterName":"Pat","requesterEmail":"pat@example.com","companyName":"Pat LLC",
                                 "title":"Old request title","details":"This request should expire during retention cleanup."}
                                """))
                .andExpect(status().isCreated());

        UUID submissionId = submissions.findAll().getFirst().getId();
        jdbc.update("update request_submission set created_at = ? where id = ?",
                Timestamp.from(Instant.now().minus(40, ChronoUnit.DAYS)), submissionId);

        retentionService.purgeExpiredRequests();
        assertThat(submissions.findAllByTenantIdOrderByCreatedAtDesc(TENANT)).isEmpty();
    }
}
