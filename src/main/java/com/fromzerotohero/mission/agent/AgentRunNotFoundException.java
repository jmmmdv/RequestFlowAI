package com.fromzerotohero.mission.agent;

import java.util.UUID;

public class AgentRunNotFoundException extends RuntimeException {
    public AgentRunNotFoundException(UUID id) { super("Could not find agent run " + id); }
}
