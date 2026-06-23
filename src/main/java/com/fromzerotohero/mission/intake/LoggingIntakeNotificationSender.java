package com.fromzerotohero.mission.intake;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingIntakeNotificationSender implements IntakeNotificationSender {
    private static final Logger log = LoggerFactory.getLogger(LoggingIntakeNotificationSender.class);

    @Override
    public void send(List<String> recipients, String subject, String body) {
        log.info("INTAKE_NOTIFICATION recipients={} subject={} body={}", recipients, subject, body);
    }
}
