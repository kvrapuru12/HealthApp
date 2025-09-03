-- Flyway Migration: Fix enum case sensitivity for food_items and food_logs tables

-- Update food_items table enum values to uppercase
ALTER TABLE food_items MODIFY COLUMN visibility ENUM('PRIVATE', 'PUBLIC') DEFAULT 'PRIVATE';
ALTER TABLE food_items MODIFY COLUMN status ENUM('ACTIVE', 'DELETED') DEFAULT 'ACTIVE';

-- Update existing data to use uppercase values
UPDATE food_items SET visibility = 'PUBLIC' WHERE visibility = 'public';
UPDATE food_items SET visibility = 'PRIVATE' WHERE visibility = 'private';
UPDATE food_items SET status = 'ACTIVE' WHERE status = 'active';
UPDATE food_items SET status = 'DELETED' WHERE status = 'deleted';

-- Update food_logs table enum values to uppercase
ALTER TABLE food_logs MODIFY COLUMN status ENUM('ACTIVE', 'DELETED') DEFAULT 'ACTIVE';

-- Update existing data to use uppercase values
UPDATE food_logs SET status = 'ACTIVE' WHERE status = 'active';
UPDATE food_logs SET status = 'DELETED' WHERE status = 'deleted';
