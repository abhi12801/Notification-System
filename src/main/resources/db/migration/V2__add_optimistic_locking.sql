-- Guards against a lost-update race between two concurrent retry requests
-- for the same notification (see NotificationServiceImpl.retry).
ALTER TABLE notifications ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
