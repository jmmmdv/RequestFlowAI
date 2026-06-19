package com.fromzerotohero.mission.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fromzerotohero.mission.workitem.WorkItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PlanningAgentTest {
    @Autowired PlanningAgent agent;
    @Autowired WorkItemRepository repository;
    @Autowired AgentRunRepository runRepository;

    @BeforeEach void clean() {
        runRepository.deleteAll();
        repository.deleteAll();
    }

    @Test void classifiesAndExecutesACloudGoal() {
        AgentResponse response = agent.run(new AgentRequest("Deploy REST API to AWS", true));
        assertThat(response.classification()).isEqualTo("DELIVERY_AUTOMATION");
        assertThat(response.createdWorkItemIds()).hasSize(3);
        assertThat(repository.count()).isEqualTo(3);
        assertThat(response.runId()).isNotNull();
        assertThat(runRepository.findById(response.runId())).get()
                .extracting(AgentRun::getOutcome, AgentRun::getCreatedWorkItems)
                .containsExactly("EXECUTED", 3);
    }

    @Test void supportsSafeDryRuns() {
        AgentResponse response = agent.run(new AgentRequest("Investigate a flaky test", false));
        assertThat(response.classification()).isEqualTo("QUALITY_ENGINEERING");
        assertThat(repository.count()).isZero();
        assertThat(runRepository.findById(response.runId())).get()
                .extracting(AgentRun::getOutcome, AgentRun::getCreatedWorkItems)
                .containsExactly("DRY_RUN", 0);
    }
}
