-- Flyway Migration: Create Sleep Logs Table

CREATE TABLE sleep_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    logged_at DATETIME(3) NOT NULL,
    hours DECIMAL(4,1) NOT NULL CHECK (hours BETWEEN 0.0 AND 24.0),
    note VARCHAR(200) NULL,
    status ENUM('active', 'deleted') NOT NULL DEFAULT 'active',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    
    -- Foreign key constraint
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_sleep_user_loggedat ON sleep_logs(user_id, logged_at);
CREATE INDEX idx_sleep_logs_status ON sleep_logs(status);
CREATE INDEX idx_sleep_logs_logged_at ON sleep_logs(logged_at);
