-- Flyway Migration: Create notification device + preference tables

CREATE TABLE notification_devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    expo_push_token VARCHAR(255) NOT NULL,
    platform VARCHAR(30) NOT NULL,
    app_version VARCHAR(100),
    build_number VARCHAR(100),
    timezone VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_notification_devices_user_device (user_id, device_id),
    UNIQUE KEY uk_notification_devices_token (expo_push_token),
    INDEX idx_notification_devices_user_status (user_id, status),
    INDEX idx_notification_devices_last_seen_at (last_seen_at),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE notification_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    food_reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    activity_reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    cycle_phase_reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_time TIME NOT NULL DEFAULT '20:00:00',
    quiet_hours_start TIME NULL,
    quiet_hours_end TIME NULL,
    timezone VARCHAR(100) NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_notification_preferences_user (user_id),
    INDEX idx_notification_preferences_timezone (timezone),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
