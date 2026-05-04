-- Allow longer food log notes (e.g. voice parsing assumptions + original transcript context)
ALTER TABLE food_logs MODIFY COLUMN note VARCHAR(2000) NULL;
