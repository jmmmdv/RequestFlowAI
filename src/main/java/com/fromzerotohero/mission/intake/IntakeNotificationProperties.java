package com.fromzerotohero.mission.intake;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mission.notifications.email")
public class IntakeNotificationProperties {
    private boolean enabled = true;
    private DeliveryMode mode = DeliveryMode.LOG;
    private String from = "notifications@localhost";
    private String fallbackRecipient = "";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public DeliveryMode getMode() { return mode; }
    public void setMode(DeliveryMode mode) { this.mode = mode; }
    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }
    public String getFallbackRecipient() { return fallbackRecipient; }
    public void setFallbackRecipient(String fallbackRecipient) { this.fallbackRecipient = fallbackRecipient; }

    public enum DeliveryMode {
        LOG,
        SMTP
    }
}
