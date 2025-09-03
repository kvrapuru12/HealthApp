-- Flyway Migration: Fix meal_type enum case sensitivity in food_logs table

-- Update food_logs table meal_type enum values to uppercase
ALTER TABLE food_logs MODIFY COLUMN meal_type ENUM('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK');

-- Update existing data to use uppercase values
UPDATE food_logs SET meal_type = 'BREAKFAST' WHERE meal_type = 'breakfast';
UPDATE food_logs SET meal_type = 'LUNCH' WHERE meal_type = 'lunch';
UPDATE food_logs SET meal_type = 'DINNER' WHERE meal_type = 'dinner';
UPDATE food_logs SET meal_type = 'SNACK' WHERE meal_type = 'snack';
