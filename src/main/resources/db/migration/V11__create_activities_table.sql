-- Create activities table for managing reusable activities
CREATE TABLE activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    calories_per_minute DECIMAL(4,2),
    visibility VARCHAR(20) NOT NULL DEFAULT 'private',
    created_by BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    -- Constraints
    CONSTRAINT fk_activities_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_activity_name_length CHECK (LENGTH(name) >= 3),
    CONSTRAINT chk_activity_calories CHECK (calories_per_minute IS NULL OR (calories_per_minute >= 0.1 AND calories_per_minute <= 50.0)),
    CONSTRAINT chk_activity_visibility CHECK (visibility IN ('private', 'public')),
    CONSTRAINT chk_activity_status CHECK (status IN ('active', 'deleted')),
    CONSTRAINT uk_activity_name_user UNIQUE (name, created_by, status)
);

-- Create indexes for performance optimization
CREATE INDEX idx_activities_created_by ON activities(created_by);
CREATE INDEX idx_activities_visibility ON activities(visibility);
CREATE INDEX idx_activities_status ON activities(status);
CREATE INDEX idx_activities_name ON activities(name);
CREATE INDEX idx_activities_category ON activities(category);
CREATE INDEX idx_activities_created_at ON activities(created_at);

-- Add comments for documentation
ALTER TABLE activities COMMENT = 'Table for managing reusable activities with visibility and calorie tracking';
