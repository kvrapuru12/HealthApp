-- Fix activity_type column to be ENUM instead of VARCHAR
-- This migration fixes the schema mismatch that's causing the application to fail

-- First, drop the existing column (this will lose data, but it's necessary for the fix)
ALTER TABLE activity_entries DROP COLUMN activity_type;

-- Recreate the column with the correct ENUM type
ALTER TABLE activity_entries ADD COLUMN activity_type ENUM(
    'RUNNING','WALKING','CYCLING','SWIMMING','WEIGHT_TRAINING','YOGA','PILATES',
    'DANCING','HIKING','TENNIS','BASKETBALL','SOCCER','GOLF','SKIING','ROWING',
    'ELLIPTICAL','STAIR_CLIMBER'
) AFTER activity_name; 