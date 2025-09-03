-- Flyway Migration: Create food_items table for Food Module

CREATE TABLE food_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    default_unit VARCHAR(20) DEFAULT 'grams',
    quantity_per_unit DOUBLE DEFAULT 100,
    calories_per_unit INT NOT NULL,
    protein_per_unit DOUBLE,
    carbs_per_unit DOUBLE,
    fat_per_unit DOUBLE,
    fiber_per_unit DOUBLE,
    visibility ENUM('private', 'public') DEFAULT 'private',
    status ENUM('active', 'deleted') DEFAULT 'active',
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for food_items table
CREATE INDEX idx_food_items_created_by ON food_items(created_by);
CREATE INDEX idx_food_items_visibility ON food_items(visibility);
CREATE INDEX idx_food_items_status ON food_items(status);
CREATE INDEX idx_food_items_name ON food_items(name);
CREATE INDEX idx_food_items_category ON food_items(category);
