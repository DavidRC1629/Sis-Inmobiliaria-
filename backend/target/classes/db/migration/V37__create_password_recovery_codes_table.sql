CREATE TABLE IF NOT EXISTS password_recovery_codes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    code VARCHAR(8) NOT NULL,
    expires_at DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_recovery_code_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_recovery_user ON password_recovery_codes(user_id);
CREATE INDEX idx_recovery_code_active ON password_recovery_codes(code, used, expires_at);
