package com.fromzerotohero.mission.agent;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.ResponseEntity;
import java.util.UUID;
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
    private final AgentOrchestrator orchestrator;
    private final AgentRunRepository runRepository;
    private final TenantContext tenantContext;

    public AgentController(AgentOrchestrator orchestrator, AgentRunRepository runRepository,
            TenantContext tenantContext) {
        this.orchestrator = orchestrator;
        this.runRepository = runRepository;
        this.tenantContext = tenantContext;
    }

    @PostMapping("/plan")
    @Operation(summary = "Plan a goal", description = "Dry-runs or creates exactly three auditable work items.")
    public ResponseEntity<AgentResponse> plan(@Valid @RequestBody AgentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        AgentResponse response = orchestrator.plan(request, idempotencyKey);
        return ResponseEntity.ok().header("Idempotency-Key", response.idempotencyKey()).body(response);
    }

    @PostMapping("/runs/{id}/approve")
    @Operation(summary = "Approve a high-impact plan",
            description = "Executes a pending plan once; repeated approval returns the same result.")
    public AgentResponse approve(@PathVariable UUID id) {
        return orchestrator.approve(id);
    }

    @GetMapping("/runs")
    @Operation(summary = "List agent audit records", description = "Requires ADMIN in production.")
    public List<AgentRun> runs() {
        return runRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantContext.tenantId());
    }
}
