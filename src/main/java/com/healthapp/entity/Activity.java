package com.healthapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "activities", indexes = {
    @Index(name = "idx_activities_created_by", columnList = "created_by"),
    @Index(name = "idx_activities_visibility", columnList = "visibility"),
    @Index(name = "idx_activities_status", columnList = "status"),
    @Index(name = "idx_activities_name", columnList = "name"),
    @Index(name = "idx_activities_category", columnList = "category"),
    @Index(name = "idx_activities_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class Activity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 100)
    @NotNull(message = "Activity name is required")
    @Size(min = 3, max = 100, message = "Activity name must be between 3 and 100 characters")
    private String name;
    
    @Column(name = "category", length = 50)
    @Size(max = 50, message = "Category cannot exceed 50 characters")
    private String category;
    
    @Column(name = "calories_per_minute", precision = 4, scale = 2)
    @DecimalMin(value = "0.1", message = "Calories per minute must be at least 0.1")
    @DecimalMax(value = "50.0", message = "Calories per minute cannot exceed 50.0")
    private BigDecimal caloriesPerMinute;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'private'")
    private Visibility visibility = Visibility.PRIVATE;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnore
    private User createdBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'active'")
    private Status status = Status.ACTIVE;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum Visibility {
        PRIVATE, PUBLIC
    }
    
    public enum Status {
        ACTIVE, DELETED
    }
    
    // Constructors
    public Activity() {}
    
    public Activity(String name, String category, BigDecimal caloriesPerMinute, Visibility visibility, User createdBy) {
        this.name = name;
        this.category = category;
        this.caloriesPerMinute = caloriesPerMinute;
        this.visibility = visibility;
        this.createdBy = createdBy;
        this.status = Status.ACTIVE;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public BigDecimal getCaloriesPerMinute() { return caloriesPerMinute; }
    public void setCaloriesPerMinute(BigDecimal caloriesPerMinute) { this.caloriesPerMinute = caloriesPerMinute; }
    
    public Visibility getVisibility() { return visibility; }
    public void setVisibility(Visibility visibility) { this.visibility = visibility; }
    
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    
    public Long getCreatedById() { return createdBy != null ? createdBy.getId() : null; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
