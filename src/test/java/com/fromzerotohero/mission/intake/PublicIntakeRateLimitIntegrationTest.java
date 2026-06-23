package com.fromzerotohero.mission.intake;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fromzerotohero.mission.saas.Plan;
import com.fromzerotohero.mission.saas.TenantOrganizationRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "mission.seed.enabled=false",
        "mission.intake.rate-limit.enabled=true",
        "mission.intake.rate-limit.requests-per-minute=2"
})
@AutoConfigureMockMvc
class PublicIntakeRateLimitIntegrationTest {
    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired MockMvc mvc;
    @Autowired TenantOrganizationRepository organizations;

    @BeforeEach
    void resetOrganization() {
        var organization = organizations.findById(TENANT).orElseThrow();
        organization.rename("Local Development", "local");
        organization.changePlan(Plan.FREE);
        organization.clearPortalToken();
        organizations.save(organization);
    }

    @Test
    void rateLimitsRepeatedPublicSubmissions() throws Exception {
        String body = """
                {"requesterName":"Alex","requesterEmail":"alex@example.com","companyName":"Alex Co",
                 "title":"Need help with billing","details":"Please review the latest invoice charge today."}
                """;
        mvc.perform(post("/api/public/intake/local").header("Idempotency-Key", "rate-limit-001")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/public/intake/local").header("Idempotency-Key", "rate-limit-002")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/public/intake/local").header("Idempotency-Key", "rate-limit-003")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value("Too many requests. Please wait a minute and try again."));
    }
}
