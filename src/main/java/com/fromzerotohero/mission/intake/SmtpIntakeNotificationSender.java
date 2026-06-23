package com.fromzerotohero.mission.intake;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class SmtpIntakeNotificationSender implements IntakeNotificationSender {
    private static final Logger log = LoggerFactory.getLogger(SmtpIntakeNotificationSender.class);

    private final JavaMailSender mailSender;
    private final IntakeNotificationProperties properties;

    public SmtpIntakeNotificationSender(JavaMailSender mailSender, IntakeNotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(List<String> recipients, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFrom());
        message.setTo(recipients.toArray(String[]::new));
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("INTAKE_NOTIFICATION sent to {} recipient(s) with subject {}", recipients.size(), subject);
    }
}
