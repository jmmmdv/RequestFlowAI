package com.fromzerotohero.mission.workitem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkItemRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 1000) String description,
        @NotNull Priority priority,
        @NotNull WorkStatus status) {
}
