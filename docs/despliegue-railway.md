# Despliegue y validación en Railway

Estado al 7 de noviembre de 2026: preparación y validación local. El proyecto existente hopeful-stillness está en production con la prueba de Railway vencida y sus tres servicios apagados. No se ha publicado todavía la versión actual ni se han repetido las pruebas en una dirección pública.

## Plataforma y servicios

El responsable eligió Railway Hobby y correo por API HTTPS, porque Hobby no permite SMTP saliente. La aplicación mantiene SMTP para el entorno local y agrega Resend como proveedor configurable por HTTPS.

Servicios previstos:

| Servicio | Origen | Configuración |
|---|---|---|
| Web Angular y Nginx | Raíz del repositorio, Dockerfile | PORT, API_HOST y CHATBOT_HOST |
| API Java | /backend, Dockerfile | Base MySQL, JWT, origen HTTPS, cookies seguras y proveedor de correo |
| MySQL persistente | Servicio existente | Respaldar antes de migrar; conservar su volumen |
| Chatbot | /chatbot, Dockerfile | BACKEND_URL con la URL HTTPS de la API |

Los dominios de API_HOST y CHATBOT_HOST no incluyen protocolo ni rutas. Nginx los usa mediante HTTPS, valida los certificados y conserva las rutas /api/ y /chat/. La versión anterior tenía una dirección de backend fija y no incluía el proxy del chatbot.

La red privada puede usarse para MySQL. La propuesta web preparada usa orígenes HTTPS de la API y del chatbot; estos conservan su autenticación. No exponer MySQL por una dirección pública salvo acceso temporal de mantenimiento.

## Configuración del correo

MAIL_PROVIDER=resend, RESEND_API_KEY privada, RESEND_FROM y ALERT_EMAIL_TO. Mantener ALERT_EMAIL_ENABLED=false hasta comprobar un único aviso y registrar la línea inicial. Con onboarding@resend.dev, Resend limita el remitente de prueba al correo asociado a la cuenta; para otros destinatarios se requiere un dominio verificado.

El código registra la aceptación del proveedor; no equivale a confirmar la bandeja de entrada. Rechazos explícitos quedan fallidos y los resultados inciertos no se reenvían automáticamente.

## Procedimiento previsto

1. Identificar con commit y etiqueta el código validado. Los cambios actuales están en la rama local codex/preparar-version-final, sobre la base 9461354a8767f7bf2ab317e485a8d77f8ce21eca. Esta base no identifica por sí sola las modificaciones locales.
2. Antes de activar la base anterior, verificar el volumen y obtener un respaldo. Probar una restauración independiente. No reemplazar los datos antiguos con la base local de demostración sin una decisión expresa.
3. Activar el plan elegido desde la cuenta del responsable. El pago lo realiza el titular.
4. Configurar las raíces de servicio, variables y secretos desde Railway. Configurar la API para escuchar en la dirección y puerto suministrados por la plataforma, APP_ORIGIN con el dominio HTTPS de la web y SECURE_COOKIE=true.
5. Iniciar MySQL y aplicar Flyway con un usuario que tenga permisos de migración. V7 recrea disparadores de perfiles y V8 añade avisos_correo. Separar las credenciales de aplicación y migración una vez validado el esquema.
6. Desplegar API y chatbot, comprobar salud y generar sus dominios. Configurar esos dominios en la web y desplegarla.
7. Comprobar acceso, alta de producto y lote, atención de stock, reportes, chatbot, alertas y correo. Usar registros claramente ficticios y conservar sus identificadores.
8. Verificar permisos de empleado y supervisor, respuestas de error, persistencia después de reiniciar y ausencia de errores de JavaScript.
9. Registrar URL pública, identificador del despliegue, commit/etiqueta, fecha, capturas y resultados. Repetir las mediciones relevantes del punto 22 en esta plataforma.

## Reversión prevista

Detener primero el envío automático si el defecto afecta al correo. En Railway puede restaurarse un despliegue previo exitoso desde su historial cuando el plan aún lo conserva; verificar también las variables restauradas.

Volver a una imagen anterior no restaura la base de datos. La tabla de V8 es adicional y puede conservarse al volver a un código que no la usa. Si el defecto requiere recuperar datos o esquema, poner el sistema en mantenimiento y restaurar el respaldo validado en una base independiente antes de cambiar la conexión. No sobrescribir directamente la base activa.

La versión estable de retorno y la retención real del historial deben identificarse al publicar. No se ha ejecutado una reversión en Railway en esta sesión.

## Validaciones locales y pendientes

El respaldo local y su restauración temporal se comprobaron, incluyendo todas las tablas físicas detectadas y avisos_correo. Los detalles quedan en docs/backup-verification.json y el respaldo privado en .local/backups.

La persistencia local tras reiniciar MySQL y la API quedó verificada por conteos y SHA256 de filas ordenadas: 94 productos, 95 lotes, 100 movimientos y 31 registros de correo idénticos antes y después. La API volvió a estado UP. Evidencia: docs/deployment-validation/persistence.json. Esto no sustituye la comprobación de persistencia en Railway.

Pasaron 36 pruebas Java/MySQL, incluidas SMTP local, aceptación HTTPS simulada, rechazo de credenciales y tratamiento de una respuesta incierta. Además, Resend aceptó por HTTPS el aviso real del lote DEMO-RESEND-1794073952789 en un intento, sin errores, y el usuario confirmó su recepción. Esta prueba se originó en el entorno local; debe repetirse desde Railway.

La imagen web preparada se construyó y pasó nginx -t con dominios de prueba. Sin dominios, el arranque se rechaza expresamente. Estas verificaciones no prueban todavía conectividad entre servicios en Railway.

Defectos y condiciones pendientes: límite compartido de 240 solicitudes por minuto observado en la prueba de pico; GitHub Actions pausado; cuenta Railway sin plan activo; URL pública, versión publicada y pruebas de producción todavía pendientes. Resend utiliza el remitente de demostración y solo puede enviar al destinatario permitido de la cuenta hasta verificar un dominio propio.

Referencias oficiales: https://docs.railway.com/networking/outbound-networking, https://docs.railway.com/deployments/monorepo, https://docs.railway.com/deployments/deployment-actions y https://resend.com/docs/api-reference/emails/send-email.
