package com.fromzerotohero.mission.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentRequest(@NotBlank @Size(max = 500) String goal, boolean createWorkItems) {
}
