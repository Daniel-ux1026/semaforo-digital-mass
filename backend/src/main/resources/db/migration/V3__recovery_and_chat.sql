ALTER TABLE app_user ADD email VARCHAR(254) NULL UNIQUE;
CREATE TABLE password_reset (
 hash CHAR(64) PRIMARY KEY, user_id BIGINT NOT NULL,
 password_snapshot VARCHAR(100) NOT NULL, email_snapshot VARCHAR(254) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL, expires_at TIMESTAMP(6) NOT NULL,
 used BOOLEAN NOT NULL DEFAULT FALSE, delivery VARCHAR(12) NOT NULL DEFAULT 'PENDING',
 FOREIGN KEY(user_id) REFERENCES app_user(id), INDEX ix_reset_user(user_id,created_at)
);
CREATE TABLE chat_session (
 hash CHAR(64) PRIMARY KEY, session_id VARCHAR(36) NOT NULL, expires_at TIMESTAMP(6) NOT NULL,
 FOREIGN KEY(session_id) REFERENCES auth_session(id)
);
ALTER TABLE alert ADD attended_by BIGINT NULL, ADD CONSTRAINT fk_alert_actor FOREIGN KEY(attended_by) REFERENCES app_user(id);
