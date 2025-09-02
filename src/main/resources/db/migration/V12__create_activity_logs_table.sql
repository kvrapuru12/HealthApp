-- Create activity_logs table for tracking user activity sessions
CREATE TABLE activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_id BIGINT NOT NULL,
    logged_at DATETIME(6) NOT NULL,
    duration_minutes INT NOT NULL,
    calories_burned DECIMAL(6,2),
    note VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    -- Constraints
    CONSTRAINT fk_activity_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_logs_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE,
    CONSTRAINT chk_activity_logs_duration CHECK (duration_minutes >= 1 AND duration_minutes <= 300),
    CONSTRAINT chk_activity_logs_calories CHECK (calories_burned IS NULL OR calories_burned >= 0),
    CONSTRAINT chk_activity_logs_status CHECK (status IN ('active', 'deleted'))
);

-- Create indexes for performance optimization
CREATE INDEX idx_activity_logs_user ON activity_logs(user_id);
CREATE INDEX idx_activity_logs_activity ON activity_logs(activity_id);
CREATE INDEX idx_activity_logs_logged_at ON activity_logs(logged_at);
CREATE INDEX idx_activity_logs_status ON activity_logs(status);
CREATE INDEX idx_activity_logs_created_at ON activity_logs(created_at);
CREATE INDEX idx_activity_logs_user_logged_at ON activity_logs(user_id, logged_at);

-- Add comments for documentation
ALTER TABLE activity_logs COMMENT = 'Table for tracking user activity sessions with calorie calculations';
