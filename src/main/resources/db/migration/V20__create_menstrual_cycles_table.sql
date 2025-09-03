-- Flyway Migration: Create menstrual_cycles table

CREATE TABLE menstrual_cycles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    period_start_date DATE NOT NULL,
    cycle_length INT DEFAULT 28,
    period_duration INT DEFAULT 5,
    is_cycle_regular BOOLEAN DEFAULT TRUE,
    status ENUM('ACTIVE', 'DELETED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_menstrual_cycles_user_id (user_id),
    INDEX idx_menstrual_cycles_period_start_date (period_start_date),
    INDEX idx_menstrual_cycles_status (status),
    INDEX idx_menstrual_cycles_user_period (user_id, period_start_date),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
