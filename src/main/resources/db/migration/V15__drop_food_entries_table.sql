-- Drop food_entries table and related objects
-- This migration removes the old food_entries table that has been replaced by a more modern food tracking system

-- Drop the table (this will automatically drop the foreign key constraint and indexes)
DROP TABLE IF EXISTS food_entries;

-- Add comment for documentation
-- Note: This migration removes the legacy food_entries table that was replaced by a more robust food tracking system
