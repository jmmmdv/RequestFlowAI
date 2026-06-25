package com.fromzerotohero.mission.ai.provider;

import com.fromzerotohero.mission.intake.RequestAnalysisInput;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DisabledAiRequestAnalysisProvider implements AiRequestAnalysisProvider {
    private static final Logger log = LoggerFactory.getLogger(DisabledAiRequestAnalysisProvider.class);

    private final AiProviderProperties properties;

    public DisabledAiRequestAnalysisProvider(AiProviderProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<AiRequestAnalysisProviderResult> analyze(RequestAnalysisInput input) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        log.info("Paid AI provider is enabled but no external implementation is wired; using rule-based fallback");
        return Optional.empty();
    }
}
