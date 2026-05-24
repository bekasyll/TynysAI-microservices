--liquibase formatted sql

--changeset tynysai:001-create-notifications
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(40) NOT NULL,
    params TEXT,
    read BOOLEAN NOT NULL,
    related_entity_id VARCHAR(64),
    related_entity_type VARCHAR(50),
    read_at TIMESTAMP,
    created_at TIMESTAMP
);

--changeset tynysai:001-create-notifications-indexes
CREATE INDEX IF NOT EXISTS idx_notification_user ON notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_notification_user_read ON notifications (user_id, read);
