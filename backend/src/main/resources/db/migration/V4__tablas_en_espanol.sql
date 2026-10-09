-- Nombres físicos en español; las vistas mantienen compatibilidad con la API.
RENAME TABLE app_user TO usuarios, store_lock TO bloqueo_tienda,
 auth_session TO sesiones, refresh_token TO renovaciones_sesion,
 product TO productos, lot TO lotes, movement TO movimientos,
 idempotency TO operaciones_idempotentes, audit TO auditoria,
 alert TO alertas, evaluation TO evaluaciones, outbox TO notificaciones,
 service_receipt TO recepciones_servicio, password_reset TO recuperaciones_clave,
 chat_session TO sesiones_chat;
CREATE SQL SECURITY INVOKER VIEW app_user AS SELECT * FROM usuarios;
CREATE SQL SECURITY INVOKER VIEW store_lock AS SELECT * FROM bloqueo_tienda;
CREATE SQL SECURITY INVOKER VIEW auth_session AS SELECT * FROM sesiones;
CREATE SQL SECURITY INVOKER VIEW refresh_token AS SELECT * FROM renovaciones_sesion;
CREATE SQL SECURITY INVOKER VIEW product AS SELECT * FROM productos;
CREATE SQL SECURITY INVOKER VIEW lot AS SELECT * FROM lotes;
CREATE SQL SECURITY INVOKER VIEW movement AS SELECT * FROM movimientos;
CREATE SQL SECURITY INVOKER VIEW idempotency AS SELECT * FROM operaciones_idempotentes;
CREATE SQL SECURITY INVOKER VIEW audit AS SELECT * FROM auditoria;
CREATE SQL SECURITY INVOKER VIEW alert AS SELECT * FROM alertas;
CREATE SQL SECURITY INVOKER VIEW evaluation AS SELECT * FROM evaluaciones;
CREATE SQL SECURITY INVOKER VIEW outbox AS SELECT * FROM notificaciones;
CREATE SQL SECURITY INVOKER VIEW service_receipt AS SELECT * FROM recepciones_servicio;
CREATE SQL SECURITY INVOKER VIEW password_reset AS SELECT * FROM recuperaciones_clave;
CREATE SQL SECURITY INVOKER VIEW chat_session AS SELECT * FROM sesiones_chat;
