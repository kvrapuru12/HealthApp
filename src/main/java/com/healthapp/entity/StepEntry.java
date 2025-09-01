package com.healthapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "step_entries", indexes = {
    @Index(name = "idx_user_logged_at", columnList = "user_id, logged_at"),
    @Index(name = "idx_logged_at", columnList = "logged_at"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Schema(description = "Step tracking entry for users")
public class StepEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier for the step entry")
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @Schema(description = "User who logged the steps")
    private User user;
    
    @Column(name = "logged_at", nullable = false)
    @NotNull(message = "Logged at time is required")
    @Schema(description = "When the steps were logged")
    private LocalDateTime loggedAt;
    
    @Column(name = "step_count", nullable = false)
    @NotNull(message = "Step count is required")
    @Min(value = 0, message = "Step count must be at least 0")
    @Max(value = 100000, message = "Step count cannot exceed 100,000")
    @Schema(description = "Number of steps logged", minimum = "0", maximum = "100000")
    private Integer stepCount;
    
    @Column(name = "note", length = 200)
    @Size(max = 200, message = "Note cannot exceed 200 characters")
    @Schema(description = "Optional note about the steps")
    private String note;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    @Schema(description = "Status of the step entry")
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
    public StepEntry() {}
    
    public StepEntry(User user, LocalDateTime loggedAt, Integer stepCount, String note) {
        this.user = user;
        this.loggedAt = loggedAt;
        this.stepCount = stepCount;
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
    
    public Integer getStepCount() { return stepCount; }
    public void setStepCount(Integer stepCount) { this.stepCount = stepCount; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
