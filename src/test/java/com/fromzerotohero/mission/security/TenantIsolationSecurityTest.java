package com.fromzerotohero.mission.security;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fromzerotohero.mission.agent.AgentRunRepository;
import com.fromzerotohero.mission.workitem.Priority;
import com.fromzerotohero.mission.workitem.WorkItem;
import com.fromzerotohero.mission.workitem.WorkItemRepository;
import com.fromzerotohero.mission.workitem.WorkStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "mission.security.enabled=true",
        "mission.seed.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused"
})
@AutoConfigureMockMvc
class TenantIsolationSecurityTest {
    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired WorkItemRepository workItems;
    @Autowired AgentRunRepository agentRuns;

    @BeforeEach
    void setUpTenants() {
        agentRuns.deleteAll();
        workItems.deleteAll();
        Integer existing = jdbc.queryForObject("select count(*) from tenant where id = ?", Integer.class, TENANT_B);
        if (existing == 0) {
            jdbc.update("insert into tenant (id, name, slug, created_at) values (?, ?, ?, current_timestamp)",
                    TENANT_B, "Second Tenant", "second");
        }
    }

    @Test
    void rejectsUnauthenticatedApiRequests() throws Exception {
        mvc.perform(get("/api/work-items"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsOnlyRowsOwnedByTheJwtTenant() throws Exception {
        List<WorkItem> saved = workItems.saveAll(List.of(
                new WorkItem("Tenant A item", "Visible", Priority.HIGH, WorkStatus.READY, TENANT_A),
                new WorkItem("Tenant B item", "Hidden", Priority.HIGH, WorkStatus.READY, TENANT_B)));

        mvc.perform(get("/api/work-items").with(jwtFor(TENANT_A, "VIEWER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.workItemList", hasSize(1)))
                .andExpect(jsonPath("$._embedded.workItemList[0].title").value("Tenant A item"));

        mvc.perform(get("/api/work-items/{id}", saved.get(1).getId()).with(jwtFor(TENANT_A, "VIEWER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void memberCanRunAgentAndAuditIsAttributedToTenantAndUser() throws Exception {
        mvc.perform(post("/api/agent/plan")
                        .with(jwtFor(TENANT_B, "MEMBER"))
                        .header("X-Correlation-ID", "security-test-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"goal":"Verify tenant security","createWorkItems":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").isNotEmpty())
                .andExpect(jsonPath("$.createdWorkItemIds", hasSize(3)));

        var runs = agentRuns.findAllByTenantIdOrderByCreatedAtDesc(TENANT_B);
        org.assertj.core.api.Assertions.assertThat(runs).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(runs.getFirst().getUserId()).isEqualTo("user-123");
        org.assertj.core.api.Assertions.assertThat(runs.getFirst().getCorrelationId()).isEqualTo("security-test-42");
        org.assertj.core.api.Assertions.assertThat(
                workItems.findAllByTenantIdOrderByUpdatedAtDesc(TENANT_A)).isEmpty();
    }

    @Test
    void viewerCannotExecuteAgent() throws Exception {
        mvc.perform(post("/api/agent/plan")
                        .with(jwtFor(TENANT_A, "VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"goal\":\"Forbidden action\",\"createWorkItems\":true}"))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtFor(UUID tenant, String role) {
        return jwt().jwt(token -> token.subject("user-123").claim("tenant_id", tenant.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
