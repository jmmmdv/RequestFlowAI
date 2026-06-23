CREATE TABLE stripe_webhook_event (
    event_id VARCHAR(255) NOT NULL PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL
);
