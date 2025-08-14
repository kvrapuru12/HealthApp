-- Flyway Migration: Create Mood Entries Table

CREATE TABLE mood_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    logged_at TIMESTAMP NOT NULL,
    mood ENUM('HAPPY', 'SAD', 'ANGRY', 'EXCITED', 'CALM', 'ANXIOUS', 'STRESSED', 'RELAXED', 
              'ENERGIZED', 'TIRED', 'FOCUSED', 'DISTRACTED', 'GRATEFUL', 'FRUSTRATED', 
              'CONTENT', 'IRRITATED', 'JOYFUL', 'MELANCHOLY', 'OPTIMISTIC', 'PESSIMISTIC') NOT NULL,
    intensity INT,
    note VARCHAR(200),
    status ENUM('ACTIVE', 'INACTIVE', 'DELETED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Add constraints
    CONSTRAINT chk_intensity CHECK (intensity IS NULL OR (intensity >= 1 AND intensity <= 10)),
    CONSTRAINT chk_note_length CHECK (note IS NULL OR CHAR_LENGTH(note) <= 200),
    
    -- Foreign key constraint
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_mood_entries_user_id ON mood_entries(user_id);
CREATE INDEX idx_mood_entries_logged_at ON mood_entries(logged_at);
CREATE INDEX idx_mood_entries_user_logged_at ON mood_entries(user_id, logged_at);
CREATE INDEX idx_mood_entries_status ON mood_entries(status);
