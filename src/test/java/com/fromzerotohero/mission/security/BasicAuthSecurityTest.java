package com.fromzerotohero.mission.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "mission.security.enabled=true",
        "mission.security.provider=basic",
        "mission.seed.enabled=false",
        "mission.tenant.default-id=00000000-0000-0000-0000-0000000000ba",
        "requestflow.admin.email=owner@example.com",
        "requestflow.admin.password-hash={noop}secret-local-test",
        "requestflow.admin.organization-name=Lean RequestFlow"
})
@AutoConfigureMockMvc
class BasicAuthSecurityTest {
    private static final UUID LOCAL_TENANT =
            UUID.fromString("00000000-0000-0000-0000-0000000000ba");

    @Autowired MockMvc mvc;

    @Test
    void protectsApiWithEnvironmentDrivenBasicAuth() throws Exception {
        mvc.perform(get("/api/saas/organization"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/saas/organization")
                        .with(httpBasic("owner@example.com", "secret-local-test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(LOCAL_TENANT.toString()))
                .andExpect(jsonPath("$.name").value("Lean RequestFlow"))
                .andExpect(jsonPath("$.currentUserRole").value("ADMIN"));
    }
}
