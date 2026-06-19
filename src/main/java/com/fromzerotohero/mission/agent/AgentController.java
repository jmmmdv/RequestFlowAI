package com.fromzerotohero.mission.agent;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import com.fromzerotohero.mission.security.TenantContext;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/agent")
@Tag(name = "Planning agent", description = "Bounded planning and tenant-scoped audit history")
@SecurityRequirement(name = "bearer-jwt")
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
    @Operation(summary = "Plan a goal", description = "Dry-runs or creates exactly three auditable work items.")
    public AgentResponse plan(@Valid @RequestBody AgentRequest request) {
        return agent.run(request);
    }

    @GetMapping("/runs")
    @Operation(summary = "List agent audit records", description = "Requires ADMIN in production.")
    public List<AgentRun> runs() {
        return runRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantContext.tenantId());
    }
}
