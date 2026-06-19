package com.fromzerotohero.mission.workitem;

public class WorkItemNotFoundException extends RuntimeException {
    public WorkItemNotFoundException(Long id) {
        super("Work item %d was not found".formatted(id));
    }
}
