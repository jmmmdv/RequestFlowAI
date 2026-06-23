package com.fromzerotohero.mission.workitem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "mission.seed.enabled=false")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlApiIntegrationTest {
    private static final UUID LOCAL_TENANT =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void flywayAndApiPersistTenantDataOnRealPostgreSql() throws Exception {
        String version = jdbc.queryForObject(
                "select version from flyway_schema_history where success order by installed_rank desc limit 1",
                String.class);
        assertThat(version).isEqualTo("8");

        Integer safetyColumns = jdbc.queryForObject("""
                select count(*) from information_schema.columns
                where table_name = 'agent_run'
                  and column_name in ('idempotency_key', 'tool_budget', 'approved_at')
                """, Integer.class);
        assertThat(safetyColumns).isEqualTo(3);

        Integer saasTables = jdbc.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public'
                  and table_name in ('tenant_membership', 'tenant_invitation', 'billing_subscription')
                """, Integer.class);
        assertThat(saasTables).isEqualTo(3);

        Integer intakeTables = jdbc.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = 'public' and table_name = 'request_submission'
                """, Integer.class);
        assertThat(intakeTables).isEqualTo(1);

        Integer intakeMetadataColumns = jdbc.queryForObject("""
                select count(*) from information_schema.columns
                where table_name = 'request_submission'
                  and column_name in ('company_name', 'requested_category', 'requested_urgency')
                """, Integer.class);
        assertThat(intakeMetadataColumns).isEqualTo(3);

        Integer portalControlColumns = jdbc.queryForObject("""
                select count(*) from information_schema.columns
                where table_name = 'tenant'
                  and column_name in ('portal_token_hash', 'request_retention_days', 'onboarding_completed')
                """, Integer.class);
        assertThat(portalControlColumns).isEqualTo(3);

        var response = mvc.perform(post("/api/work-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"PostgreSQL evidence","description":"Real engine test",
                                 "priority":"HIGH","status":"READY"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse();

        JsonNode api = objectMapper.readTree(response.getContentAsString());
        UUID storedTenant = jdbc.queryForObject(
                "select tenant_id from work_item where id = ?", UUID.class, api.path("id").asLong());
        assertThat(storedTenant).isEqualTo(LOCAL_TENANT);
        assertThat(jdbc.queryForObject("select current_database()", String.class)).isEqualTo("test");
    }
}
