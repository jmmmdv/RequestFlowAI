package com.fromzerotohero.mission.config;

import com.fromzerotohero.mission.intake.PublicIntakeProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PublicIntakeProperties.class)
public class IntakeConfig {}
