package com.fromzerotohero.mission.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AgentRequest(@NotBlank @Size(max = 500) String goal, boolean createWorkItems,
                           @Min(1) @Max(3) Integer toolBudget) {
    public AgentRequest(String goal, boolean createWorkItems) {
        this(goal, createWorkItems, 3);
    }

    public int effectiveToolBudget() {
        return toolBudget == null ? 3 : toolBudget;
    }
}
