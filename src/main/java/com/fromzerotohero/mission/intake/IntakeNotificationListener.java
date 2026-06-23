package com.fromzerotohero.mission.intake;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class IntakeNotificationListener {
    private final IntakeNotificationService notifications;

    public IntakeNotificationListener(IntakeNotificationService notifications) {
        this.notifications = notifications;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNewPublicRequest(NewPublicRequestEvent event) {
        notifications.notifyNewRequest(notifications.toNotification(event));
    }
}
