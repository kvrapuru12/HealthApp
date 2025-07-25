-- Flyway Migration: Initial Schema for HealthApp

-- Users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone_number VARCHAR(20),
    date_of_birth TIMESTAMP NULL,
    gender ENUM('MALE', 'FEMALE', 'OTHER'),
    height_cm DOUBLE,
    weight_kg DOUBLE,
    age INT,
    activity_level ENUM('SEDENTARY', 'LIGHTLY_ACTIVE', 'MODERATELY_ACTIVE', 'VERY_ACTIVE', 'EXTREMELY_ACTIVE'),
    daily_calorie_goal INT DEFAULT 2000,
    role ENUM('ADMIN', 'USER') DEFAULT 'USER',
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Activity Entries table
CREATE TABLE activity_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_name VARCHAR(100) NOT NULL,
    activity_type ENUM('RUNNING','WALKING','CYCLING','SWIMMING','WEIGHT_TRAINING','YOGA','PILATES','DANCING','HIKING','TENNIS','BASKETBALL','SOCCER','GOLF','SKIING','ROWING','ELLIPTICAL','STAIR_CLIMBER'),
    intensity_level ENUM('LOW','MODERATE','HIGH','VERY_HIGH'),
    duration_minutes INT,
    calories_burned INT,
    activity_date DATE,
    start_time TIMESTAMP NULL,
    end_time TIMESTAMP NULL,
    distance_km DOUBLE,
    steps INT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Food Entries table
CREATE TABLE food_entries (
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
    meal_type ENUM('BREAKFAST','LUNCH','DINNER','SNACK'),
    consumption_date DATE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_activity_entries_user_id ON activity_entries(user_id);
CREATE INDEX idx_food_entries_user_id ON food_entries(user_id); 