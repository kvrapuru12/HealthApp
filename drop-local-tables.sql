-- Drop all tables in local healthapp database
-- WARNING: This will permanently delete all data!

USE healthapp;

-- Disable foreign key checks temporarily
SET FOREIGN_KEY_CHECKS = 0;

-- Drop all tables
DROP TABLE IF EXISTS activity_entries;
DROP TABLE IF EXISTS food_entries;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS flyway_schema_history;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;

-- Verify tables are dropped
SHOW TABLES; 