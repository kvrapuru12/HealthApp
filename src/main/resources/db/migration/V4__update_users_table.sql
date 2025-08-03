-- Flyway Migration: Update Users Table Structure

-- Drop existing foreign key constraints first
ALTER TABLE activity_entries DROP FOREIGN KEY activity_entries_ibfk_1;
ALTER TABLE food_entries DROP FOREIGN KEY food_entries_ibfk_1;

-- Drop existing indexes
DROP INDEX idx_users_username ON users;

-- Drop the existing users table
DROP TABLE users;

-- Recreate users table with new structure
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50),
    phone_number VARCHAR(20),
    email VARCHAR(100) NOT NULL UNIQUE,
    username VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(64) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender ENUM('FEMALE', 'MALE', 'NON_BINARY', 'OTHER') NOT NULL,
    activity_level ENUM('SEDENTARY', 'LIGHT', 'MODERATE', 'ACTIVE', 'VERY_ACTIVE') NOT NULL,
    daily_calorie_intake_target INT,
    daily_calorie_burn_target INT,
    weight_kg DOUBLE,
    height_cm DOUBLE,
    role ENUM('USER', 'ADMIN', 'COACH') NOT NULL,
    account_status ENUM('ACTIVE', 'INACTIVE', 'DELETED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Add constraints
    CONSTRAINT chk_daily_calorie_intake_target CHECK (daily_calorie_intake_target IS NULL OR (daily_calorie_intake_target >= 800 AND daily_calorie_intake_target <= 6000)),
    CONSTRAINT chk_daily_calorie_burn_target CHECK (daily_calorie_burn_target IS NULL OR (daily_calorie_burn_target >= 100 AND daily_calorie_burn_target <= 3000)),
    CONSTRAINT chk_weight CHECK (weight_kg IS NULL OR (weight_kg >= 30 AND weight_kg <= 300)),
    CONSTRAINT chk_height CHECK (height_cm IS NULL OR (height_cm >= 100 AND height_cm <= 250)),
    CONSTRAINT chk_phone_number CHECK (phone_number IS NULL OR phone_number REGEXP '^\\+?[1-9]\\d{1,14}$'),
    CONSTRAINT chk_username CHECK (username REGEXP '^[a-zA-Z0-9_]+$'),
    CONSTRAINT chk_first_name CHECK (first_name REGEXP '^[a-zA-Z\\s]*$'),
    CONSTRAINT chk_last_name CHECK (last_name IS NULL OR last_name REGEXP '^[a-zA-Z\\s]*$'),
    CONSTRAINT chk_email CHECK (email REGEXP '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$')
);

-- Recreate indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- Recreate foreign key constraints
ALTER TABLE activity_entries ADD CONSTRAINT activity_entries_ibfk_1 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE food_entries ADD CONSTRAINT food_entries_ibfk_1 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE; 