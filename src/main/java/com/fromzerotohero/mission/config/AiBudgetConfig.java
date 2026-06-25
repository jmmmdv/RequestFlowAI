package com.fromzerotohero.mission.config;

import com.fromzerotohero.mission.ai.budget.AiBudgetProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiBudgetProperties.class)
public class AiBudgetConfig {}
