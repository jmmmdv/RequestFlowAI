package com.fromzerotohero.mission.workitem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API and database consistency")
class ApiDatabaseConsistencyTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired WorkItemRepository repository;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/work-items/{id} matches the raw database row")
    void singleApiResourceMatchesDatabaseRow() throws Exception {
        WorkItem saved = repository.saveAndFlush(new WorkItem(
                "Compare API and DB", "Seeded directly in the database layer",
                Priority.HIGH, WorkStatus.READY));

        DbWorkItem database = findDatabaseRow(saved.getId());
        JsonNode api = getJson("/api/work-items/" + saved.getId());

        assertApiMatchesDatabase(api, database, true);
        report("DB -> API single-resource comparison", database, api);
    }

    @Test
    @DisplayName("GET collection contains exactly the rows stored in the database")
    void apiCollectionMatchesAllDatabaseRows() throws Exception {
        repository.saveAllAndFlush(List.of(
                new WorkItem("First item", "Database sample A", Priority.LOW, WorkStatus.BACKLOG),
                new WorkItem("Second item", "Database sample B", Priority.CRITICAL, WorkStatus.IN_PROGRESS)));

        List<DbWorkItem> databaseRows = findAllDatabaseRows();
        JsonNode apiItems = getJson("/api/work-items").path("_embedded").path("workItemList");
        Map<Long, JsonNode> apiById = new HashMap<>();
        apiItems.forEach(item -> apiById.put(item.path("id").asLong(), item));

        assertThat(apiItems.size()).as("API count must equal database count").isEqualTo(databaseRows.size());
        databaseRows.forEach(row -> {
            assertThat(apiById).as("API must contain database id %s", row.id()).containsKey(row.id());
            assertApiMatchesDatabase(apiById.get(row.id()), row, true);
        });

        System.out.printf("%n[CONSISTENCY PASS] DB -> API collection: database=%d, api=%d%n",
                databaseRows.size(), apiItems.size());
    }

    @Test
    @DisplayName("POST response and persisted database row contain the same business data")
    void apiCreateMatchesPersistedDatabaseRow() throws Exception {
        MvcResult result = mvc.perform(post("/api/work-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Created through API","description":"Must persist unchanged",
                                 "priority":"MEDIUM","status":"BACKLOG"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode api = objectMapper.readTree(result.getResponse().getContentAsString());
        DbWorkItem database = findDatabaseRow(api.path("id").asLong());

        assertApiMatchesDatabase(api, database, false);
        assertThat(result.getResponse().getHeader("Location"))
                .as("Location header must identify the persisted row")
                .endsWith("/api/work-items/" + database.id());
        report("API -> DB create comparison", database, api);
    }

    @Test
    @DisplayName("PUT changes the database and DELETE removes the same row")
    void apiUpdateAndDeleteRemainConsistentWithDatabase() throws Exception {
        WorkItem saved = repository.saveAndFlush(new WorkItem(
                "Before update", "Old value", Priority.LOW, WorkStatus.BACKLOG));

        MvcResult update = mvc.perform(put("/api/work-items/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"After update","description":"New value",
                                 "priority":"HIGH","status":"DONE"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode api = objectMapper.readTree(update.getResponse().getContentAsString());
        DbWorkItem database = findDatabaseRow(saved.getId());
        assertApiMatchesDatabase(api, database, false);
        report("API -> DB update comparison", database, api);

        mvc.perform(delete("/api/work-items/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        Integer rowsRemaining = jdbc.queryForObject(
                "select count(*) from work_item where id = ?", Integer.class, saved.getId());
        assertThat(rowsRemaining).as("Deleted API resource must not remain in the database").isZero();
        System.out.printf("[CONSISTENCY PASS] API -> DB delete: id=%d, database rows remaining=%d%n",
                saved.getId(), rowsRemaining);
    }

    private JsonNode getJson(String path) throws Exception {
        MvcResult result = mvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private DbWorkItem findDatabaseRow(Long id) {
        return jdbc.queryForObject("""
                        select id, title, description, priority, status, updated_at
                        from work_item where id = ?
                        """, (rs, rowNum) -> new DbWorkItem(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        Priority.valueOf(rs.getString("priority")),
                        WorkStatus.valueOf(rs.getString("status")),
                        rs.getTimestamp("updated_at").toInstant()), id);
    }

    private List<DbWorkItem> findAllDatabaseRows() {
        return jdbc.query("""
                select id, title, description, priority, status, updated_at
                from work_item order by id
                """, (rs, rowNum) -> new DbWorkItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("description"),
                Priority.valueOf(rs.getString("priority")),
                WorkStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("updated_at").toInstant()));
    }

    private void assertApiMatchesDatabase(JsonNode api, DbWorkItem database, boolean compareTimestamp) {
        assertThat(api.path("id").asLong()).as("id").isEqualTo(database.id());
        assertThat(api.path("title").asText()).as("title").isEqualTo(database.title());
        assertThat(api.path("description").asText()).as("description").isEqualTo(database.description());
        assertThat(api.path("priority").asText()).as("priority").isEqualTo(database.priority().name());
        assertThat(api.path("status").asText()).as("status").isEqualTo(database.status().name());
        if (compareTimestamp) {
            assertThat(Instant.parse(api.path("updatedAt").asText()))
                    .as("updatedAt").isEqualTo(database.updatedAt());
        }
    }

    private void report(String scenario, DbWorkItem database, JsonNode api) {
        System.out.printf("""

                [CONSISTENCY PASS] %s
                  field       database                  api
                  id          %-25s %s
                  title       %-25s %s
                  priority    %-25s %s
                  status      %-25s %s
                %n""",
                scenario,
                database.id(), api.path("id").asLong(),
                database.title(), api.path("title").asText(),
                database.priority(), api.path("priority").asText(),
                database.status(), api.path("status").asText());
    }

    private record DbWorkItem(Long id, String title, String description, Priority priority,
                              WorkStatus status, Instant updatedAt) {
    }
}
