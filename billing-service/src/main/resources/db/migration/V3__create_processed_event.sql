-- One row per Kafka event already handled, so redelivered events are skipped.
CREATE TABLE processed_event (
    event_id     VARCHAR(64)              PRIMARY KEY,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
