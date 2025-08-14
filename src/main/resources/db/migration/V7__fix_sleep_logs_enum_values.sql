-- Flyway Migration: Fix Sleep Logs Enum Values

-- First, update existing records to use uppercase values
UPDATE sleep_logs SET status = 'ACTIVE' WHERE status = 'active';
UPDATE sleep_logs SET status = 'DELETED' WHERE status = 'deleted';

-- Then modify the table to use uppercase enum values
ALTER TABLE sleep_logs MODIFY COLUMN status ENUM('ACTIVE', 'DELETED') NOT NULL DEFAULT 'ACTIVE';
