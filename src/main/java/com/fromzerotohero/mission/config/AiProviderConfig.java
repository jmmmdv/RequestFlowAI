package com.fromzerotohero.mission.config;

import com.fromzerotohero.mission.ai.provider.AiProviderProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiProviderProperties.class)
public class AiProviderConfig {}
