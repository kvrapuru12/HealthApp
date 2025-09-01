-- Create water_entries table for tracking daily water consumption
CREATE TABLE water_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    logged_at DATETIME(6) NOT NULL,
    amount INT NOT NULL,
    note VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    
    -- Constraints
    CONSTRAINT fk_water_entries_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_water_amount_range CHECK (amount >= 10 AND amount <= 5000),
    CONSTRAINT chk_water_status CHECK (status IN ('ACTIVE', 'DELETED'))
);

-- Create indexes for performance optimization
CREATE INDEX idx_water_user_logged_at ON water_entries(user_id, logged_at);
CREATE INDEX idx_water_logged_at ON water_entries(logged_at);
CREATE INDEX idx_water_status ON water_entries(status);
CREATE INDEX idx_water_created_at ON water_entries(created_at);

-- Add comments for documentation
ALTER TABLE water_entries COMMENT = 'Table for tracking user daily water consumption in milliliters with validation and soft delete support';
