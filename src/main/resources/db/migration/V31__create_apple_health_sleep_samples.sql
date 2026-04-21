-- Apple Health: sleep analysis segments (idempotent per HealthKit external id)
CREATE TABLE apple_health_sleep_samples (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    external_sample_id VARCHAR(512) NOT NULL,
    local_date DATE NOT NULL,
    period_start_utc DATETIME(6) NOT NULL,
    period_end_utc DATETIME(6) NOT NULL,
    sleep_stage VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_apple_health_sleep_samples_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_apple_health_sleep_user_external UNIQUE (user_id, external_sample_id)
);

CREATE INDEX idx_apple_health_sleep_user_local_date ON apple_health_sleep_samples(user_id, local_date);

ALTER TABLE apple_health_sleep_samples COMMENT = 'Apple Health sleep category samples; upserted by external_sample_id per user';
