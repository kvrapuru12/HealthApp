-- Flyway Migration: Create food_logs table for Food Module

CREATE TABLE food_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    food_item_id BIGINT NOT NULL,
    logged_at TIMESTAMP NOT NULL,
    meal_type ENUM('breakfast', 'lunch', 'dinner', 'snack'),
    quantity DOUBLE NOT NULL,
    unit VARCHAR(20) NOT NULL,
    calories DOUBLE,
    protein DOUBLE,
    carbs DOUBLE,
    fat DOUBLE,
    fiber DOUBLE,
    note VARCHAR(200),
    status ENUM('active', 'deleted') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (food_item_id) REFERENCES food_items(id) ON DELETE CASCADE
);

-- Indexes for food_logs table
CREATE INDEX idx_food_logs_user_id ON food_logs(user_id);
CREATE INDEX idx_food_logs_food_item_id ON food_logs(food_item_id);
CREATE INDEX idx_food_logs_logged_at ON food_logs(logged_at);
CREATE INDEX idx_food_logs_meal_type ON food_logs(meal_type);
CREATE INDEX idx_food_logs_status ON food_logs(status);
