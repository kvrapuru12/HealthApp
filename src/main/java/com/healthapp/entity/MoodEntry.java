package com.healthapp.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Entity
@Table(name = "mood_entries")
@EntityListeners(AuditingEntityListener.class)
@Schema(description = "Mood entry entity for tracking user emotional states and wellness")
public class MoodEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Auto-generated primary key", example = "1")
    private Long id;
    

    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Schema(description = "User who created this mood entry")
    private User user;
    
    @Column(name = "logged_at", nullable = false)
    @Schema(description = "Timestamp when the mood was logged", example = "2025-08-12T07:30:00Z")
    private LocalDateTime loggedAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "mood", nullable = false)
    @Schema(description = "The emotional state being tracked", example = "HAPPY")
    private Mood mood;
    
    @Min(value = 1, message = "Intensity must be between 1 and 10")
    @Max(value = 10, message = "Intensity must be between 1 and 10")
    @Column(name = "intensity")
    @Schema(description = "Intensity level on a 1-10 scale", example = "6")
    private Integer intensity;
    
    @Size(max = 200, message = "Note cannot exceed 200 characters")
    @Column(name = "note", columnDefinition = "VARCHAR(200)")
    @Schema(description = "Optional note about the mood", example = "Feeling great after morning workout!")
    private String note;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Schema(description = "Current status of the mood entry", example = "ACTIVE")
    private Status status = Status.ACTIVE;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Timestamp when the entry was created", example = "2025-08-12T07:30:00Z")
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Timestamp when the entry was last updated", example = "2025-08-12T07:30:00Z")
    private LocalDateTime updatedAt;
    
    // Constructors
    public MoodEntry() {}
    
    public MoodEntry(User user, LocalDateTime loggedAt, Mood mood) {
        this.user = user;
        this.loggedAt = loggedAt;
        this.mood = mood;
    }
    
    public MoodEntry(User user, LocalDateTime loggedAt, Mood mood, Integer intensity, String note) {
        this.user = user;
        this.loggedAt = loggedAt;
        this.mood = mood;
        this.intensity = intensity;
        this.note = note;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    

    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }
    
    public void setLoggedAt(LocalDateTime loggedAt) {
        this.loggedAt = loggedAt;
    }
    
    public Mood getMood() {
        return mood;
    }
    
    public void setMood(Mood mood) {
        this.mood = mood;
    }
    
    public Integer getIntensity() {
        return intensity;
    }
    
    public void setIntensity(Integer intensity) {
        this.intensity = intensity;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Enums
    @Schema(description = "Available mood states for tracking emotional wellness")
    public enum Mood {
        @Schema(description = "Feeling joyful and positive")
        HAPPY,
        @Schema(description = "Feeling down or unhappy")
        SAD,
        @Schema(description = "Feeling mad or irritated")
        ANGRY,
        @Schema(description = "Feeling enthusiastic and eager")
        EXCITED,
        @Schema(description = "Feeling peaceful and relaxed")
        CALM,
        @Schema(description = "Feeling worried or uneasy")
        ANXIOUS,
        @Schema(description = "Feeling overwhelmed or pressured")
        STRESSED,
        @Schema(description = "Feeling at ease and comfortable")
        RELAXED,
        @Schema(description = "Feeling full of vitality and power")
        ENERGIZED,
        @Schema(description = "Feeling sleepy or exhausted")
        TIRED,
        @Schema(description = "Feeling concentrated and attentive")
        FOCUSED,
        @Schema(description = "Feeling unfocused or scattered")
        DISTRACTED,
        @Schema(description = "Feeling thankful and appreciative")
        GRATEFUL,
        @Schema(description = "Feeling annoyed or discouraged")
        FRUSTRATED,
        @Schema(description = "Feeling satisfied and fulfilled")
        CONTENT,
        @Schema(description = "Feeling easily annoyed or bothered")
        IRRITATED,
        @Schema(description = "Feeling delighted and cheerful")
        JOYFUL,
        @Schema(description = "Feeling sad and thoughtful")
        MELANCHOLY,
        @Schema(description = "Feeling hopeful and positive about the future")
        OPTIMISTIC,
        @Schema(description = "Feeling negative about the future")
        PESSIMISTIC
    }
    
    @Schema(description = "Status of the mood entry")
    public enum Status {
        @Schema(description = "Entry is active and visible")
        ACTIVE,
        @Schema(description = "Entry is inactive but preserved")
        INACTIVE,
        @Schema(description = "Entry has been deleted")
        DELETED
    }
}
