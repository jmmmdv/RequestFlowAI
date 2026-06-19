package com.fromzerotohero.mission.workitem;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WorkItemControllerTest {
    @Autowired MockMvc mvc;
    @Autowired WorkItemRepository repository;

    @BeforeEach void clean() { repository.deleteAll(); }

    @Test void createsAndReturnsHypermediaWorkItem() throws Exception {
        mvc.perform(post("/api/work-items").contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"Ship it","description":"Test first","priority":"HIGH","status":"READY"}
                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.title").value("Ship it"))
                .andExpect(jsonPath("$._links.self.href").exists());

        mvc.perform(get("/api/work-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.workItemList", hasSize(1)));
    }

    @Test void returnsProblemDetailForMissingItem() throws Exception {
        mvc.perform(get("/api/work-items/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Work item not found"));
    }

    @Test void rejectsInvalidInput() throws Exception {
        mvc.perform(post("/api/work-items").contentType(MediaType.APPLICATION_JSON).content("""
                {"title":"","priority":"HIGH","status":"READY"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test void changesOnlyStatusAndReturnsTraceHeader() throws Exception {
        WorkItem item = repository.saveAndFlush(new WorkItem(
                "Traceable work", "Keep this description", Priority.MEDIUM, WorkStatus.BACKLOG));

        mvc.perform(patch("/api/work-items/{id}/status", item.getId())
                        .header("X-Correlation-ID", "test-run-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", "test-run-42"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.description").value("Keep this description"));
    }
}
