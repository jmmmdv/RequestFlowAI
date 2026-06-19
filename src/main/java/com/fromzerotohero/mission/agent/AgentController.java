package com.fromzerotohero.mission.agent;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import com.fromzerotohero.mission.security.TenantContext;
import java.util.List;

@RestController
@RequestMapping("/api/agent")
public class AgentController {
    private final PlanningAgent agent;
    private final AgentRunRepository runRepository;
    private final TenantContext tenantContext;

    public AgentController(PlanningAgent agent, AgentRunRepository runRepository, TenantContext tenantContext) {
        this.agent = agent;
        this.runRepository = runRepository;
        this.tenantContext = tenantContext;
    }

    @PostMapping("/plan")
    public AgentResponse plan(@Valid @RequestBody AgentRequest request) {
        return agent.run(request);
    }

    @GetMapping("/runs")
    public List<AgentRun> runs() {
        return runRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantContext.tenantId());
    }
}
