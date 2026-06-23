package com.fromzerotohero.mission.saas;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "stripe_webhook_event")
public class BillingWebhookEvent {
    @Id
    private String eventId;
    private Instant processedAt;

    protected BillingWebhookEvent() {}

    BillingWebhookEvent(String eventId, Instant processedAt) {
        this.eventId = eventId;
        this.processedAt = processedAt;
    }
}
