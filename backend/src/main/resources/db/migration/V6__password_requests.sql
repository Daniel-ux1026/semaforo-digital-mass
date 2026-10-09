UPDATE usuarios SET must_change=false,version=version+1 WHERE role IN ('WORKER','SUPERVISOR');
UPDATE recuperaciones_clave r JOIN usuarios u ON u.id=r.user_id SET r.used=true WHERE u.role<>'ADMIN';
CREATE TABLE solicitudes_clave (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,usuario_id BIGINT NOT NULL,estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',creada_el TIMESTAMP(6) NOT NULL,resuelta_el TIMESTAMP(6),resuelta_por BIGINT,
 pendiente_usuario BIGINT GENERATED ALWAYS AS (CASE WHEN estado='PENDIENTE' THEN usuario_id ELSE NULL END) STORED,
 UNIQUE KEY uq_solicitud_pendiente(pendiente_usuario),FOREIGN KEY(usuario_id) REFERENCES usuarios(id),FOREIGN KEY(resuelta_por) REFERENCES usuarios(id),CHECK(estado IN ('PENDIENTE','RESUELTA')));
