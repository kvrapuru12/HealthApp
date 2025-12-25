-- Add google_id column to users table for Google Sign-In support
ALTER TABLE users 
ADD COLUMN google_id VARCHAR(255) UNIQUE NULL AFTER email;

CREATE INDEX idx_users_google_id ON users(google_id);

