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
    phone_number VARCHAR(20),
    date_of_birth TIMESTAMP,
    gender VARCHAR(10),
    height_cm DOUBLE,
    weight_kg DOUBLE,
    age INT,
    activity_level VARCHAR(20),
    daily_calorie_goal INT DEFAULT 2000,
    role VARCHAR(10) DEFAULT 'USER',
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Food entries table
CREATE TABLE IF NOT EXISTS food_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    food_name VARCHAR(100) NOT NULL,
    calories INT,
    protein_g DOUBLE,
    carbs_g DOUBLE,
    fat_g DOUBLE,
    fiber_g DOUBLE,
    serving_size VARCHAR(50),
    quantity DOUBLE,
    meal_type VARCHAR(20),
    consumption_date DATE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Activity entries table
CREATE TABLE IF NOT EXISTS activity_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_name VARCHAR(100) NOT NULL,
    calories_burned INT,
    duration_minutes INT,
    activity_type VARCHAR(50),
    intensity_level VARCHAR(20),
    activity_date DATE,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    distance_km DOUBLE,
    steps INT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_food_entries_user_id ON food_entries(user_id);
CREATE INDEX IF NOT EXISTS idx_food_entries_consumption_date ON food_entries(consumption_date);
CREATE INDEX IF NOT EXISTS idx_activity_entries_user_id ON activity_entries(user_id);
CREATE INDEX IF NOT EXISTS idx_activity_entries_activity_date ON activity_entries(activity_date);

-- Insert sample data (optional - for development/testing)
INSERT IGNORE INTO users (username, email, password, first_name, last_name, age, weight_kg, height_cm, activity_level, daily_calorie_goal, role, enabled) VALUES
('demo_user', 'demo@healthapp.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Demo', 'User', 30, 70.5, 175.0, 'MODERATELY_ACTIVE', 2000, 'USER', true);

-- Insert sample food entries
INSERT IGNORE INTO food_entries (user_id, food_name, calories, protein_g, carbs_g, fat_g, fiber_g, meal_type, consumption_date) VALUES
(1, 'Oatmeal with Berries', 250, 8.5, 45.2, 4.1, 6.8, 'BREAKFAST', CURDATE()),
(1, 'Grilled Chicken Salad', 320, 35.0, 12.5, 15.2, 8.5, 'LUNCH', CURDATE()),
(1, 'Salmon with Vegetables', 450, 38.2, 18.7, 22.1, 12.3, 'DINNER', CURDATE());

-- Insert sample activity entries
INSERT IGNORE INTO activity_entries (user_id, activity_name, calories_burned, duration_minutes, activity_type, intensity_level, activity_date) VALUES
(1, 'Morning Run', 350, 30, 'RUNNING', 'HIGH', CURDATE()),
(1, 'Weight Training', 280, 45, 'WEIGHT_TRAINING', 'MODERATE', CURDATE()),
(1, 'Yoga Session', 120, 60, 'YOGA', 'LOW', CURDATE()); 