-- Add target fields to users table
ALTER TABLE users 
ADD COLUMN target_fat DOUBLE COMMENT 'Target fat intake in grams per day',
ADD COLUMN target_protein DOUBLE COMMENT 'Target protein intake in grams per day',
ADD COLUMN target_carbs DOUBLE COMMENT 'Target carbs intake in grams per day',
ADD COLUMN target_sleep_hours DOUBLE COMMENT 'Target sleep hours per day',
ADD COLUMN target_water_litres DOUBLE COMMENT 'Target water intake in litres per day',
ADD COLUMN target_steps INTEGER COMMENT 'Target steps per day',
ADD COLUMN target_weight DOUBLE COMMENT 'Target weight in kg',
ADD COLUMN last_period_date DATE COMMENT 'Last period date for female users';
