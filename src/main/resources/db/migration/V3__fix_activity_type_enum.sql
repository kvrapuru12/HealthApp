-- Fix activity_type column to be ENUM type
ALTER TABLE activity_entries MODIFY COLUMN activity_type ENUM(
    'running', 'walking', 'cycling', 'swimming', 'weight_training', 
    'yoga', 'pilates', 'dancing', 'hiking', 'tennis', 'basketball', 
    'soccer', 'golf', 'skiing', 'rowing', 'elliptical', 'stair_climber'
) NOT NULL; 