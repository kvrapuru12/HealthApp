-- Purge old food/activity data and keep only newer records.
-- Cutoff is interpreted as UTC.
-- Records at or before the cutoff are considered old and removed.

SET @cutoff_utc = '2026-03-07 13:30:00';

START TRANSACTION;

-- Delete old logs first (child tables).
DELETE FROM food_logs
WHERE created_at <= @cutoff_utc
   OR logged_at <= @cutoff_utc;

DELETE FROM activity_logs
WHERE created_at <= @cutoff_utc
   OR logged_at <= @cutoff_utc;

-- Delete old master rows only when no active references remain.
DELETE FROM food_items fi
WHERE fi.created_at <= @cutoff_utc
  AND NOT EXISTS (
      SELECT 1
      FROM food_logs fl
      WHERE fl.food_item_id = fi.id
  );

DELETE FROM activities a
WHERE a.created_at <= @cutoff_utc
  AND NOT EXISTS (
      SELECT 1
      FROM activity_logs al
      WHERE al.activity_id = a.id
  );

COMMIT;
