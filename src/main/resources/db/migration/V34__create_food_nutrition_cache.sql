CREATE TABLE food_nutrition_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    normalized_name VARCHAR(120) NOT NULL,
    fdc_id INT NULL,
    source VARCHAR(20) NOT NULL,
    calories_per_100g DOUBLE NOT NULL,
    protein_per_100g DOUBLE NULL,
    carbs_per_100g DOUBLE NULL,
    fat_per_100g DOUBLE NULL,
    fiber_per_100g DOUBLE NULL,
    confidence DOUBLE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_food_nutrition_cache_name (normalized_name)
);

ALTER TABLE food_items ADD COLUMN fdc_id INT NULL;
