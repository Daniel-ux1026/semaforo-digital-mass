CREATE TABLE app_user (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, dni VARCHAR(8) NOT NULL UNIQUE,
 name VARCHAR(100) NOT NULL, password VARCHAR(100) NOT NULL,
 role VARCHAR(20) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 must_change BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT ck_dni CHECK (dni REGEXP '^[0-9]{8}$'),
 CONSTRAINT ck_role CHECK (role IN ('ADMIN','WORKER'))
);
CREATE TABLE store_lock (id INT PRIMARY KEY);
INSERT INTO store_lock VALUES (1);
CREATE TABLE auth_session (
 id VARCHAR(36) PRIMARY KEY, user_id BIGINT NOT NULL, expires_at TIMESTAMP(6) NOT NULL,
 revoked BOOLEAN NOT NULL DEFAULT FALSE, FOREIGN KEY(user_id) REFERENCES app_user(id)
);
CREATE TABLE refresh_token (
 hash VARCHAR(64) PRIMARY KEY, session_id VARCHAR(36) NOT NULL, used BOOLEAN NOT NULL DEFAULT FALSE,
 FOREIGN KEY(session_id) REFERENCES auth_session(id)
);
CREATE TABLE product (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, sku VARCHAR(40) NOT NULL UNIQUE,
 ean VARCHAR(13) UNIQUE, name VARCHAR(120) NOT NULL, category VARCHAR(80) NOT NULL,
 unit VARCHAR(12) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 warning_days INT NOT NULL DEFAULT 30, critical_days INT NOT NULL DEFAULT 15,
 version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT ck_threshold CHECK (critical_days >= 0 AND warning_days > critical_days AND warning_days <= 3650),
 CONSTRAINT ck_unit CHECK (unit IN ('UNIDAD','KG','LITRO')),
 CONSTRAINT ck_ean CHECK (ean IS NULL OR ean REGEXP '^[0-9]{13}$')
);
CREATE TABLE lot (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, product_id BIGINT NOT NULL,
 code VARCHAR(60) NOT NULL, received DATE NOT NULL, expiry DATE NOT NULL,
 initial_quantity DECIMAL(14,3) NOT NULL, normal DECIMAL(14,3) NOT NULL,
 promo DECIMAL(14,3) NOT NULL DEFAULT 0, cost DECIMAL(14,4),
 created_by BIGINT NOT NULL, created_at TIMESTAMP(6) NOT NULL, version BIGINT NOT NULL DEFAULT 0,
 FOREIGN KEY(product_id) REFERENCES product(id), FOREIGN KEY(created_by) REFERENCES app_user(id),
 UNIQUE(product_id,code,expiry), INDEX ix_lot_expiry(expiry,id),
 CONSTRAINT ck_lot_qty CHECK (initial_quantity > 0 AND normal >= 0 AND promo >= 0),
 CONSTRAINT ck_cost CHECK (cost IS NULL OR cost >= 0), CONSTRAINT ck_dates CHECK (expiry >= received)
);
CREATE TABLE movement (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, lot_id BIGINT NOT NULL, actor_id BIGINT NOT NULL,
 type VARCHAR(20) NOT NULL, source VARCHAR(10) NOT NULL, quantity DECIMAL(14,3) NOT NULL,
 normal_delta DECIMAL(14,3) NOT NULL, promo_delta DECIMAL(14,3) NOT NULL,
 cost DECIMAL(14,4), reason VARCHAR(500) NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 reversal_of BIGINT UNIQUE,
 FOREIGN KEY(lot_id) REFERENCES lot(id), FOREIGN KEY(actor_id) REFERENCES app_user(id),
 FOREIGN KEY(reversal_of) REFERENCES movement(id), INDEX ix_movement_date(created_at,lot_id),
 CONSTRAINT ck_movement_type CHECK (type IN ('INGRESO','PROMOCION','VENTA','MERMA','AJUSTE','REVERSO'))
);
CREATE TABLE idempotency (
 actor_id BIGINT NOT NULL, request_key VARCHAR(36) NOT NULL, fingerprint VARCHAR(64) NOT NULL,
 result_id BIGINT NOT NULL, PRIMARY KEY(actor_id,request_key), FOREIGN KEY(actor_id) REFERENCES app_user(id)
);
CREATE TABLE audit (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, actor_id BIGINT, action VARCHAR(60) NOT NULL,
 entity VARCHAR(80) NOT NULL, detail VARCHAR(1500) NOT NULL, created_at TIMESTAMP(6) NOT NULL,
 FOREIGN KEY(actor_id) REFERENCES app_user(id), INDEX ix_audit_date(created_at)
);
CREATE TABLE alert (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, lot_id BIGINT NOT NULL, state VARCHAR(12) NOT NULL,
 attended_at TIMESTAMP(6), created_at TIMESTAMP(6) NOT NULL,
 UNIQUE(lot_id,state), FOREIGN KEY(lot_id) REFERENCES lot(id)
);
CREATE TABLE evaluation (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, day DATE NOT NULL, started_at TIMESTAMP(6) NOT NULL,
 finished_at TIMESTAMP(6), lots INT NOT NULL DEFAULT 0, status VARCHAR(20) NOT NULL, error VARCHAR(300),
 INDEX ix_evaluation_day(day,status)
);
CREATE TABLE outbox (
 id VARCHAR(36) PRIMARY KEY, alert_id BIGINT NOT NULL UNIQUE, status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 attempts INT NOT NULL DEFAULT 0, next_attempt TIMESTAMP(6) NOT NULL,
 last_error VARCHAR(300), accepted_at TIMESTAMP(6), delivered_at TIMESTAMP(6),
 FOREIGN KEY(alert_id) REFERENCES alert(id), INDEX ix_outbox_due(status,next_attempt)
);
