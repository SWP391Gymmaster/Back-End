USE swp391_group3;

-- Replace every placeholder before running. Generate the hash with BCryptPasswordEncoder.
-- Never store the raw password in this file.
INSERT INTO users (user_name, full_name, email, password_hash, phone, role, enabled, email_verified)
VALUES ('REPLACE_USERNAME', 'REPLACE_FULL_NAME', 'REPLACE_EMAIL',
        'REPLACE_WITH_BCRYPT_HASH', NULL, 'ADMIN', TRUE, TRUE);
