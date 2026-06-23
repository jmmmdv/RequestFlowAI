package com.fromzerotohero.mission.intake;

import java.util.List;

public interface IntakeNotificationSender {
    void send(List<String> recipients, String subject, String body);
}
