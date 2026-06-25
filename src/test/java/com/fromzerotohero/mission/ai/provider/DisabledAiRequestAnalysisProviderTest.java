package com.fromzerotohero.mission.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.fromzerotohero.mission.intake.RequestAnalysisInput;
import org.junit.jupiter.api.Test;

class DisabledAiRequestAnalysisProviderTest {
    @Test
    void returnsEmptyWhenProviderDisabled() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setEnabled(false);
        DisabledAiRequestAnalysisProvider provider = new DisabledAiRequestAnalysisProvider(properties);

        assertThat(provider.analyze(sampleInput())).isEmpty();
    }

    @Test
    void returnsEmptyWhenEnabledButNotImplemented() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setEnabled(true);
        DisabledAiRequestAnalysisProvider provider = new DisabledAiRequestAnalysisProvider(properties);

        assertThat(provider.analyze(sampleInput())).isEmpty();
    }

    private RequestAnalysisInput sampleInput() {
        return new RequestAnalysisInput("Booking site outage",
                "Urgent: customers cannot access the booking form.", null, null);
    }
}
