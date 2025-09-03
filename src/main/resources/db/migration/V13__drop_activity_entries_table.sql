-- Drop activity_entries table and related objects
-- This migration removes the old activity_entries table that has been replaced by activities and activity_logs tables

-- Drop the table (this will automatically drop the foreign key constraint and indexes)
DROP TABLE IF EXISTS activity_entries;

-- Add comment for documentation
-- Note: This migration removes the legacy activity_entries table that was replaced by the more robust activities and activity_logs tables
