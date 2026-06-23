package com.fromzerotohero.mission.intake;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mission.intake")
public class PublicIntakeProperties {
    private boolean honeypotEnabled = true;
    private RateLimit rateLimit = new RateLimit();

    public boolean isHoneypotEnabled() { return honeypotEnabled; }
    public void setHoneypotEnabled(boolean honeypotEnabled) { this.honeypotEnabled = honeypotEnabled; }
    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }

    public static class RateLimit {
        private boolean enabled = false;
        private int requestsPerMinute = 10;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getRequestsPerMinute() { return requestsPerMinute; }
        public void setRequestsPerMinute(int requestsPerMinute) { this.requestsPerMinute = requestsPerMinute; }
    }
}
