package com.fromzerotohero.mission.agent;

import java.util.List;
import java.util.UUID;

public record AgentResponse(UUID runId, String goal, String classification, String outcome,
                            boolean approvalRequired, String idempotencyKey, int toolBudget,
                            List<String> reasoningTrace, List<Long> createdWorkItemIds) {
}
