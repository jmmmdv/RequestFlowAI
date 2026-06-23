package com.fromzerotohero.mission.intake;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import org.junit.jupiter.api.Test;

class LoggingIntakeNotificationSenderTest {
    @Test
    void logsWithoutThrowing() {
        LoggingIntakeNotificationSender sender = new LoggingIntakeNotificationSender();

        assertThatCode(() -> sender.send(List.of("owner@example.com"), "New request", "Body text"))
                .doesNotThrowAnyException();
    }
}
