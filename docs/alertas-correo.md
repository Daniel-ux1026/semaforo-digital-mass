# Alertas de vencimiento por correo

El servidor revisa cada minuto los lotes críticos y vencidos con stock y producto activo. Envía producto, lote, fecha, plazo y saldo al destinatario configurado. Funciona con el navegador cerrado, siempre que la API y MySQL continúen encendidos.

## Configuración privada para Gmail

1. Activa la verificación en dos pasos de Google y crea una contraseña de aplicación nueva en https://myaccount.google.com/apppasswords. No utilices la contraseña normal ni una clave que haya aparecido en el chat.
2. Desde la carpeta del proyecto, ejecuta en PowerShell:

   ```powershell
   ./scripts/configure-email.ps1
   ```

   El ingreso oculta la clave y guarda `.local/email.env`, excluido de Git. Usa el Gmail acordado como remitente y destinatario. El archivo contiene SMTP con STARTTLS en el puerto 587 y envío automático desactivado.

3. Para activar los avisos, cambia `ALERT_EMAIL_ENABLED=false` a `true` en ese archivo. Al activar se incluyen también los lotes actuales que ya sean críticos o vencidos: revisar primero el inventario de demostración.
4. Aplica la configuración en el entorno local actual:

   ```powershell
   $env:COMPOSE_PROJECT_NAME='semaforo-final-1107'
   docker compose --env-file infra/.env --env-file .local/email.env -f infra/compose.yml -f .local/final-compose.yml --profile full up -d --build --no-deps api
   node scripts/harden-db.mjs
   ```

   En otra plataforma utiliza sus variables privadas. No subas el archivo ni pegues su contenido en GitHub.

## Historial y duplicados

La migración V8 agrega `avisos_correo`. Su clave única por lote, estado y destinatario evita repetir avisos, incluso tras reiniciar el servidor. Un lote que pasa de crítico a vencido recibe un nuevo aviso. No se reenvía periódicamente el mismo estado aunque cambie el stock.

Se procesan hasta cinco mensajes por ronda. El tiempo total depende de la cola y la respuesta SMTP. Las notificaciones existentes de n8n conservan su cola independiente.

En **Automatización → Alertas por correo**, el administrador puede consultar los últimos 50 avisos:

- PENDING: pendiente.
- PROCESSING: envío en curso.
- ACCEPTED: el servidor SMTP aceptó el mensaje. No confirma que llegó a la bandeja de entrada.
- FAILED: fallo identificado; por ejemplo, autenticación rechazada.
- UNCERTAIN: no se puede confirmar si salió. No se reenvía automáticamente, para evitar duplicados.
- OBSOLETE: el lote quedó sin stock, el producto se desactivó o su estado cambió.

Los errores de conexión previos al envío se reintentan hasta tres veces, con cinco minutos de espera. Un envío interrumpido queda sin confirmar al vencer su reserva de cinco minutos. SMTP no permite garantizar entrega exactamente una vez.

Después de corregir un fallo, el administrador puede reintentar un aviso FAILED mediante `POST /api/admin/email-alerts/{id}/retry`, con autenticación y CSRF. Los estados UNCERTAIN requieren revisar el proveedor antes de tomar una decisión.

## Verificación y reversión

`EmailAlertsIntegrationTest` usa MySQL real y Mailpit en contenedores aislados. Comprueba el contenido recibido por SMTP, la ausencia de duplicados, el cambio a vencido, el tratamiento de envíos inciertos y la exclusión de lotes agotados. Mailpit captura mensajes; no entrega correos a Gmail.

Para detener los envíos, configura `ALERT_EMAIL_ENABLED=false` y recrea la API. No borres el historial: conserva la deduplicación para una futura activación. Una versión anterior de la API ignora la nueva tabla; no es necesario eliminarla.

Actualización del 7 de noviembre de 2026: Gmail aceptó un aviso de prueba del lote AVISO-1794068367648 y el usuario confirmó su recepción. No se registra la contraseña en esta documentación.

El usuario eligió iniciar solo con avisos nuevos. Se registraron 28 avisos existentes como BASELINE y se activó el envío automático en el archivo privado del entorno local. La configuración real no forma parte del repositorio.

Prueba automática final: se registró el lote ficticio DEMO-AUTO-CORREO-1794069262884 y, sin usar el endpoint de prueba, el programador lo detectó y Gmail lo aceptó en un intento. Permanecieron 28 avisos antiguos sin envío. La recepción del primer mensaje fue confirmada por el usuario; el segundo tiene aceptación SMTP verificada.

Validaciones: 33 pruebas Java aprobadas, incluidas MySQL y captura SMTP real en Mailpit; 11 pruebas de interfaz aprobadas; compilación y tipos correctos. La prueba de navegador del panel de correo pasó en escritorio y móvil. Evidencias locales: `docs/evidence/email-status-desktop.png` y `docs/evidence/email-status-mobile.png`.

El endpoint administrativo `POST /api/admin/email-alerts/test/{lotId}` permite probar un solo lote al destinatario configurado, sin activar el envío automático y conservando la misma deduplicación.

Si se desea comenzar solo con avisos nuevos, antes de activar se puede registrar la línea inicial mediante `POST /api/admin/email-alerts/baseline`. Conserva los avisos existentes como BASELINE (sin envío) y respeta los ya aceptados. Un cambio posterior de crítico a vencido genera un aviso nuevo.

Referencias: [Contraseñas de aplicación de Google](https://support.google.com/accounts/answer/185833?hl=es) y [SMTP de Gmail](https://developers.google.com/workspace/gmail/imap/imap-smtp).
