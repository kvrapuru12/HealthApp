-- Hibernate maps String @Column(length=64) to VARCHAR; align with V28 CHAR(64)
ALTER TABLE refresh_tokens MODIFY COLUMN token_hash VARCHAR(64) NOT NULL COMMENT 'SHA-256 hex of opaque refresh token';
