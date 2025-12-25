-- Flyway Migration: Create App Ratings Table

CREATE TABLE app_ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    feedback TEXT,
    platform VARCHAR(10) NOT NULL,
    app_version VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Add constraints
    CONSTRAINT chk_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT chk_platform CHECK (platform IN ('ios', 'android', 'web')),
    
    -- Foreign key constraint
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_app_ratings_user_id ON app_ratings(user_id);
CREATE INDEX idx_app_ratings_created_at ON app_ratings(created_at);
CREATE INDEX idx_app_ratings_platform ON app_ratings(platform);

