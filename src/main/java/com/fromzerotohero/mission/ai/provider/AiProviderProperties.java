package com.fromzerotohero.mission.ai.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "requestflow.ai.provider")
public class AiProviderProperties {
    private boolean enabled = false;
    private String openaiApiKey = "";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getOpenaiApiKey() { return openaiApiKey; }
    public void setOpenaiApiKey(String openaiApiKey) { this.openaiApiKey = openaiApiKey; }
    public boolean hasOpenaiApiKey() { return openaiApiKey != null && !openaiApiKey.isBlank(); }
}
