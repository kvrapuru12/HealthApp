package com.healthapp.repository;

import com.healthapp.entity.FoodNutritionCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodNutritionCacheRepository extends JpaRepository<FoodNutritionCache, Long> {

    Optional<FoodNutritionCache> findByNormalizedName(String normalizedName);
}
