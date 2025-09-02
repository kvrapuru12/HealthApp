package com.healthapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "weight_logs", indexes = {
    @Index(name = "idx_weight_user_logged_at", columnList = "user_id, logged_at"),
    @Index(name = "idx_weight_logged_at", columnList = "logged_at"),
    @Index(name = "idx_weight_status", columnList = "status"),
    @Index(name = "idx_weight_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Schema(description = "Weight measurement tracking entry for users")
public class WeightEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier for the weight entry")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @Schema(description = "User who logged the weight measurement")
    private User user;
    
    @Column(name = "logged_at", nullable = false)
    @NotNull(message = "Logged at time is required")
    @Schema(description = "When the weight was measured")
    private LocalDateTime loggedAt;
    
    @Column(name = "weight", nullable = false, precision = 5, scale = 2)
    @NotNull(message = "Weight is required")
    @DecimalMin(value = "30.0", message = "Weight must be at least 30 kg")
    @DecimalMax(value = "300.0", message = "Weight cannot exceed 300 kg")
    @Schema(description = "Weight in kilograms", minimum = "30.0", maximum = "300.0")
    private BigDecimal weight;
    
    @Column(name = "note", length = 200)
    @Size(max = 200, message = "Note cannot exceed 200 characters")
    @Schema(description = "Optional note about the weight measurement")
    private String note;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    @Schema(description = "Status of the weight entry")
    private Status status = Status.ACTIVE;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "When the entry was created")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    @Schema(description = "When the entry was last updated")
    private LocalDateTime updatedAt;
    
    public enum Status {
        ACTIVE, DELETED
    }
    
    // Constructors
    public WeightEntry() {}
    
    public WeightEntry(User user, LocalDateTime loggedAt, BigDecimal weight, String note) {
        this.user = user;
        this.loggedAt = loggedAt;
        this.weight = weight;
        this.note = note;
        this.status = Status.ACTIVE;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Long getUserId() { return user != null ? user.getId() : null; }
    
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
    
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
