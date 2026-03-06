-- Add apple_id column to users table for Sign in with Apple support
ALTER TABLE users
ADD COLUMN apple_id VARCHAR(255) UNIQUE NULL AFTER google_id;

CREATE INDEX idx_users_apple_id ON users(apple_id);
