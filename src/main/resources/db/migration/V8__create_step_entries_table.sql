-- Create step_entries table for tracking daily step counts
CREATE TABLE step_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    logged_at DATETIME(6) NOT NULL,
    step_count INT NOT NULL,
    note VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    
    -- Constraints
    CONSTRAINT fk_step_entries_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_step_count_range CHECK (step_count >= 0 AND step_count <= 100000),
    CONSTRAINT chk_step_status CHECK (status IN ('ACTIVE', 'DELETED'))
);

-- Create indexes for performance optimization
CREATE INDEX idx_user_logged_at ON step_entries(user_id, logged_at);
CREATE INDEX idx_logged_at ON step_entries(logged_at);
CREATE INDEX idx_status ON step_entries(status);
CREATE INDEX idx_created_at ON step_entries(created_at);

-- Add comments for documentation
ALTER TABLE step_entries COMMENT = 'Table for tracking user daily step counts with validation and soft delete support';
