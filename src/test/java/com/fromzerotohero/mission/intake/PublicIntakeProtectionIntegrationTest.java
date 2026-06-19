package com.fromzerotohero.mission.intake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fromzerotohero.mission.saas.Plan;
import com.fromzerotohero.mission.saas.TenantOrganizationRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "mission.seed.enabled=false")
@AutoConfigureMockMvc
class PublicIntakeProtectionIntegrationTest {
    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired MockMvc mvc;
    @Autowired TenantOrganizationRepository organizations;
    @Autowired PortalTokenHasher tokenHasher;

    @BeforeEach
    void resetOrganization() {
        restoreOrganization();
    }

    @AfterEach
    void cleanupOrganization() {
        restoreOrganization();
    }

    private void restoreOrganization() {
        var organization = organizations.findById(TENANT).orElseThrow();
        organization.rename("Local Development", "local");
        organization.changePlan(Plan.FREE);
        organization.clearPortalToken();
        organizations.save(organization);
    }

    @Test
    void rejectsHoneypotSubmissions() throws Exception {
        mvc.perform(post("/api/public/intake/local").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"requesterName":"Bot","requesterEmail":"bot@example.com","companyName":"Bots",
                                 "title":"Spam request title","details":"This looks like an automated submission.",
                                 "website":"https://spam.example"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request could not be processed"));
    }

    @Test
    void protectedPortalRequiresAccessToken() throws Exception {
        var organization = organizations.findById(TENANT).orElseThrow();
        organization.rotatePortalToken(tokenHasher.hash("private-portal-token-1234567890"));
        organizations.save(organization);

        mvc.perform(get("/api/public/intake/local"))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/public/intake/local").param("token", "private-portal-token-1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portalTokenRequired").value(true));
    }

    @Test
    void portalSettingsExposeTokenRequirementFlag() throws Exception {
        mvc.perform(get("/api/public/intake/local"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portalTokenRequired").value(false));
        assertThat(tokenHasher.matches("private-portal-token-1234567890",
                tokenHasher.hash("private-portal-token-1234567890"))).isTrue();
    }
}
