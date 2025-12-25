package com.healthapp.repository;

import com.healthapp.entity.AppRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppRatingRepository extends JpaRepository<AppRating, Long> {
    
    // Spring Data JPA automatically resolves findByUserId by looking at @JoinColumn(name = "user_id")
    List<AppRating> findByUser_Id(Long userId);
    
    List<AppRating> findByPlatform(String platform);
    
    List<AppRating> findByUser_IdOrderByCreatedAtDesc(Long userId);
}

