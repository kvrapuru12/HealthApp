-- Apple Health: normalized step samples (idempotent per HealthKit external id)
CREATE TABLE apple_health_step_samples (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    external_sample_id VARCHAR(512) NOT NULL,
    local_date DATE NOT NULL,
    period_start_utc DATETIME(6) NOT NULL,
    period_end_utc DATETIME(6) NOT NULL,
    step_count INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_apple_health_step_samples_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_apple_health_step_count CHECK (step_count >= 0 AND step_count <= 1000000),
    CONSTRAINT uk_apple_health_user_external UNIQUE (user_id, external_sample_id)
);

CREATE INDEX idx_apple_health_user_local_date ON apple_health_step_samples(user_id, local_date);

ALTER TABLE apple_health_step_samples COMMENT = 'Apple Health step quantity samples; upserted by external_sample_id per user';
