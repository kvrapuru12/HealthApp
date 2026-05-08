-- One-off MySQL script: hard-delete all food_logs, activity_logs for a user, then owned food_items / activities
-- when nothing references them (matches API behavior; preserves shared public items/activities still in use).
--
-- SET THE EMAIL, verify SELECT returns one user id, then run inside a transaction.

SET @target_email = 'REPLACE_WITH_TARGET_EMAIL@example.com';

START TRANSACTION;

SELECT id, email FROM users WHERE email = @target_email;

-- After confirming exactly one row above, set @uid from that id (example below — replace with actual id if needed):
-- SET @uid = 123;

SELECT id INTO @uid FROM users WHERE email = @target_email LIMIT 1;

DELETE FROM food_logs WHERE user_id = @uid;

DELETE FROM food_items fi
WHERE fi.created_by = @uid
  AND NOT EXISTS (
      SELECT 1 FROM food_logs fl WHERE fl.food_item_id = fi.id
  );

DELETE FROM activity_logs WHERE user_id = @uid;

DELETE FROM activities a
WHERE a.created_by = @uid
  AND NOT EXISTS (
      SELECT 1 FROM activity_logs al WHERE al.activity_id = a.id
  );

COMMIT;
