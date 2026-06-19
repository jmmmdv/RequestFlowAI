package com.fromzerotohero.mission.workitem;

import jakarta.validation.constraints.NotNull;

public record WorkItemStatusRequest(@NotNull WorkStatus status) {
}
