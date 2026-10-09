CREATE TABLE service_receipt (
 event_id VARCHAR(36) PRIMARY KEY,
 accepted_at TIMESTAMP(6) NOT NULL,
 FOREIGN KEY(event_id) REFERENCES outbox(id)
);
