-- Create weight_logs table for tracking user weight measurements
CREATE TABLE weight_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    logged_at DATETIME(6) NOT NULL,
    weight DECIMAL(5,2) NOT NULL,
    note VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    
    -- Constraints
    CONSTRAINT fk_weight_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_weight_range CHECK (weight >= 30.0 AND weight <= 300.0),
    CONSTRAINT chk_weight_status CHECK (status IN ('ACTIVE', 'DELETED'))
);

-- Create indexes for performance optimization
CREATE INDEX idx_weight_user_logged_at ON weight_logs(user_id, logged_at);
CREATE INDEX idx_weight_logged_at ON weight_logs(logged_at);
CREATE INDEX idx_weight_status ON weight_logs(status);
CREATE INDEX idx_weight_created_at ON weight_logs(created_at);

-- Add comments for documentation
ALTER TABLE weight_logs COMMENT = 'Table for tracking user weight measurements in kilograms with validation and soft delete support';
