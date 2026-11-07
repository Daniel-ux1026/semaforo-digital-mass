CREATE TABLE avisos_correo (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 lote_id BIGINT NOT NULL,
 estado_lote VARCHAR(12) NOT NULL,
 destinatario VARCHAR(254) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 estado VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 intentos INT NOT NULL DEFAULT 0,
 siguiente_intento TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 aceptado_en TIMESTAMP(6) NULL,
 ultimo_error VARCHAR(60) NULL,
 creado_en TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 CONSTRAINT fk_correo_lote FOREIGN KEY(lote_id) REFERENCES lotes(id),
 CONSTRAINT uq_correo_lote UNIQUE(lote_id,estado_lote,destinatario),
 INDEX ix_correo_pendiente(estado,siguiente_intento)
);
