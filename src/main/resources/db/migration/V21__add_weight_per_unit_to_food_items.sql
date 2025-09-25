-- Add weight_per_unit column to food_items table
-- This represents the weight (in grams) of one unit of the food item

ALTER TABLE food_items 
ADD COLUMN weight_per_unit DOUBLE DEFAULT 100.0 COMMENT 'Weight in grams per unit (e.g., 150 for 1 apple)';

-- Update existing records with estimated weights based on common food items
UPDATE food_items SET weight_per_unit = 150.0 WHERE name LIKE '%apple%';
UPDATE food_items SET weight_per_unit = 50.0 WHERE name LIKE '%egg%';
UPDATE food_items SET weight_per_unit = 250.0 WHERE name LIKE '%coffee%' OR name LIKE '%tea%';
UPDATE food_items SET weight_per_unit = 30.0 WHERE name LIKE '%cashew%' OR name LIKE '%nut%';
UPDATE food_items SET weight_per_unit = 200.0 WHERE name LIKE '%salad%';
UPDATE food_items SET weight_per_unit = 100.0 WHERE name LIKE '%hash%' OR name LIKE '%brown%';

-- Set NOT NULL constraint after updating existing data
ALTER TABLE food_items 
MODIFY COLUMN weight_per_unit DOUBLE NOT NULL DEFAULT 100.0;
