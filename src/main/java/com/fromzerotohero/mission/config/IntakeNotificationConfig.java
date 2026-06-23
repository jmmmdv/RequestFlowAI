package com.fromzerotohero.mission.config;

import com.fromzerotohero.mission.intake.IntakeNotificationProperties;
import com.fromzerotohero.mission.intake.IntakeNotificationSender;
import com.fromzerotohero.mission.intake.LoggingIntakeNotificationSender;
import com.fromzerotohero.mission.intake.SmtpIntakeNotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
@EnableConfigurationProperties(IntakeNotificationProperties.class)
public class IntakeNotificationConfig {
    private static final Logger log = LoggerFactory.getLogger(IntakeNotificationConfig.class);

    @Bean
    IntakeNotificationSender intakeNotificationSender(IntakeNotificationProperties properties,
            ObjectProvider<JavaMailSender> mailSender) {
        if (properties.getMode() == IntakeNotificationProperties.DeliveryMode.SMTP) {
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender != null) {
                return new SmtpIntakeNotificationSender(sender, properties);
            }
            log.warn("mission.notifications.email.mode=smtp but JavaMailSender is unavailable; using log sender");
        }
        return new LoggingIntakeNotificationSender();
    }
}
