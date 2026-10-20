USE swp391_group3;

ALTER TABLE users MODIFY COLUMN role VARCHAR(20) NOT NULL;
ALTER TABLE users MODIFY COLUMN user_name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;
ALTER TABLE users MODIFY COLUMN full_name VARCHAR(100) NOT NULL;
ALTER TABLE users MODIFY COLUMN email VARCHAR(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;
ALTER TABLE users MODIFY COLUMN phone VARCHAR(30) NULL;
ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE users SET enabled = TRUE, email_verified = TRUE;
UPDATE users SET email = LOWER(TRIM(email)), user_name = TRIM(user_name);

CREATE TABLE email_verification_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    email_snapshot VARCHAR(254) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    consumed_at TIMESTAMP(6) NULL,
    last_requested_at TIMESTAMP(6) NOT NULL,
    window_started_at TIMESTAMP(6) NOT NULL,
    requests_in_window INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_verification_user UNIQUE (user_id),
    CONSTRAINT uk_verification_hash UNIQUE (token_hash),
    CONSTRAINT fk_verification_user FOREIGN KEY (user_id) REFERENCES users (id)
);
