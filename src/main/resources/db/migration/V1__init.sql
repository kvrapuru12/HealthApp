-- HealthApp Database Initialization Script
-- This script will be run automatically by Spring Boot Flyway

-- Create database if not exists (for local development)
-- CREATE DATABASE IF NOT EXISTS healthapp;
-- USE healthapp;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    age INT,
    weight DECIMAL(5,2),
    height DECIMAL(5,2),
    activity_level VARCHAR(20),
    daily_calorie_goal INT DEFAULT 2000,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Food entries table
CREATE TABLE IF NOT EXISTS food_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    food_name VARCHAR(100) NOT NULL,
    calories INT NOT NULL,
    protein DECIMAL(5,2),
    carbs DECIMAL(5,2),
    fat DECIMAL(5,2),
    fiber DECIMAL(5,2),
    meal_type VARCHAR(20),
    consumed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Activity entries table
CREATE TABLE IF NOT EXISTS activity_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_name VARCHAR(100) NOT NULL,
    calories_burned INT NOT NULL,
    duration_minutes INT NOT NULL,
    activity_type VARCHAR(50),
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_food_entries_user_id ON food_entries(user_id);
CREATE INDEX IF NOT EXISTS idx_food_entries_consumed_at ON food_entries(consumed_at);
CREATE INDEX IF NOT EXISTS idx_activity_entries_user_id ON activity_entries(user_id);
CREATE INDEX IF NOT EXISTS idx_activity_entries_performed_at ON activity_entries(performed_at);

-- Insert sample data (optional - for development/testing)
INSERT IGNORE INTO users (username, email, password, first_name, last_name, age, weight, height, activity_level, daily_calorie_goal) VALUES
('demo_user', 'demo@healthapp.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Demo', 'User', 30, 70.5, 175.0, 'MODERATE', 2000);

-- Insert sample food entries
INSERT IGNORE INTO food_entries (user_id, food_name, calories, protein, carbs, fat, fiber, meal_type) VALUES
(1, 'Oatmeal with Berries', 250, 8.5, 45.2, 4.1, 6.8, 'BREAKFAST'),
(1, 'Grilled Chicken Salad', 320, 35.0, 12.5, 15.2, 8.5, 'LUNCH'),
(1, 'Salmon with Vegetables', 450, 38.2, 18.7, 22.1, 12.3, 'DINNER');

-- Insert sample activity entries
INSERT IGNORE INTO activity_entries (user_id, activity_name, calories_burned, duration_minutes, activity_type) VALUES
(1, 'Morning Run', 350, 30, 'CARDIO'),
(1, 'Weight Training', 280, 45, 'STRENGTH'),
(1, 'Yoga Session', 120, 60, 'FLEXIBILITY'); 