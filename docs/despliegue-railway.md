Se desplegó Semáforo Digital Mass en Railway Hobby y se validó su funcionamiento en una dirección pública el 7 de noviembre de 2026. Se comprobó el flujo principal, el control de acceso, el manejo de errores, el correo y la conservación de datos después de reiniciar los servicios. Las pruebas utilizaron registros de demostración identificados con DEMO.

## Plataforma y versión publicada

| Elemento | Configuración comprobada |
| --- | --- |
| Plataforma | Railway Hobby; proyecto hopeful-stillness; ambiente production; región iad. |
| Acceso | https://semaforo-digital-mass-production.up.railway.app/login |
| Versión funcional | Etiqueta v1.1.0; commit 5cb4fbf2e841debd39938bd7d5a5234041f0615e. Rama codex/preparar-version-final. |
| Servicios | Angular y Nginx; API Spring Boot / Java 21; MySQL 9.7.4; chatbot Streamlit. |
| Responsable | Stiven Bravo Malca, responsable de la demostración y del despliegue. |

La etiqueta identifica el código ejecutable. Los resultados y la documentación se incorporan después en un commit documental. Los servicios de aplicación se desplegaron por SHA explícito; la etiqueta evita depender de cambios futuros de una rama.

Figura 1. Captura real de Railway con los cuatro servicios en línea y el volumen MySQL asociado. Inventario de despliegues: docs/deployment-validation/railway-services.json.

## Procedimiento de despliegue aplicado

1. Se identificó el código por commit, se publicó en GitHub y se conservó la base anterior. Se activó Hobby con autorización del titular y se configuraron los límites de consumo.

2. Antes de migrar, se obtuvo un respaldo privado de la base de Railway. Se restauró en una base local temporal y se compararon las 20 tablas físicas. Se conservó el volumen existente de Railway; no se cargó sobre él la base local.

3. Se separaron las cuentas semaforo_migrator y semaforo_app. Flyway aplicó V7 para los disparadores de perfiles y V8 para el historial de correo. El usuario de ejecución no tiene DELETE ni UPDATE sobre movimientos o auditoría.

4. Se configuraron las raíces de los Dockerfile, puertos, comprobaciones de salud y variables privadas. Las contraseñas de MySQL, JWT_SECRET, RESEND_API_KEY y OPENAI_API_KEY permanecen fuera del repositorio.

5. Se desplegaron API y chatbot, y después la web con sus dominios configurados. Se corrigieron la profundidad de validación TLS, el puerto de Streamlit y su dirección pública para aceptar el WebSocket desde la web, manteniendo CORS y XSRF activos.

6. Se creó una cuenta ADMIN ficticia autorizada para la demostración, conservando las cuentas anteriores. Se ejecutaron las pruebas de interfaz y API, el correo real, la persistencia y la regresión. La clave SSH temporal se revocó al terminar el mantenimiento.

| Servicio | Raíz y puerto | Configuración relevante |
| --- | --- | --- |
| Web | / · 8080 | API_HOST y CHATBOT_HOST sin protocolo ni ruta; proxy HTTPS con verificación TLS y profundidad 3. |
| API | /backend · 8080 | APP_ORIGIN público; SECURE_COOKIE=true; SERVER_ADDRESS=0.0.0.0; JVM 128–384 MiB. |
| MySQL | Volumen · 3306 | /var/lib/mysql; DNS privado mysql.railway.internal; base railway; buffer InnoDB 128 MiB. |
| Chatbot | /chatbot · 8501 | BACKEND_URL por HTTPS; OPENAI_MODEL=gpt-4.1-mini. STREAMLIT_BROWSER_SERVER_ADDRESS con dominio web; STREAMLIT_BROWSER_SERVER_PORT=443. |

MySQL utiliza una imagen fijada por digest, registrada en railway-services.json. No se añadió un acceso público a la base. El despliegue requiere la configuración privada además del código.

## Pruebas del flujo principal

El recorrido automatizado inició sesión desde la web pública, registró productos y lotes ficticios, consultó el inventario, atendió stock y promociones, descargó reportes CSV y verificó la presentación móvil. No se observaron errores de JavaScript en ese recorrido.

| Comprobación | Resultado |
| --- | --- |
| Acceso y catálogo | Sesión válida; registro y consulta de productos y lotes. |
| Atención y reportes | Operaciones de stock y promociones; descarga de CSV. |
| Interfaz móvil y asistente local | Contenido accesible; consultas estructuradas operativas. |

Figura 2. Captura final del inventario de demostración en la dirección pública, después de registrar el lote automático. El recorrido de Playwright consta en flow-results.json: 1 caso aprobado en 31,2 segundos. Las operaciones se ejecutaron contra la API y su almacenamiento en Railway.

## Permisos y manejo de errores

| Caso probado | Respuesta observada |
| --- | --- |
| Consultar lotes o usuarios sin sesión | HTTP 401; no se permite acceder a información protegida. |
| Empleado intenta administración | HTTP 403 en usuarios, visor de base y consulta administrativa; no se exponen costos. |
| Contraseña restablecida o usuario desactivado | Las sesiones anteriores pierden acceso. |
| DNI inválido y token vencido o alterado | HTTP 400 para formato inválido; HTTP 401 para tokens no válidos. |
| Exceso de intentos de inicio de sesión | HTTP 429; el control limita nuevos intentos. |
| Escáner, cámara y desconexión | Se prueban código inválido/desconocido/conocido, cámara denegada y operaciones deshabilitadas sin conexión. |

Figura 3. Vista real del empleado en Railway. Evidencias: permissions-errors-results.json y security-results.json. La cuenta ficticia de empleado se desactivó al terminar la prueba.

## Alertas y correo en Railway

El aviso visual muestra el lote que requiere atención. Se comprobó su presentación móvil, activación del sonido y cierre sin duplicar el aviso. El correo se envía desde una tarea programada de la API, sin n8n y sin depender de que el navegador permanezca abierto.

Primero se envió el lote DEMO-RAILWAY-CORREO-1794087292055 desde Railway y el destinatario confirmó su recepción. Luego se reconocieron cuatro avisos anteriores como BASELINE, sin enviarlos, y se activó el envío automático para avisos nuevos.

La prueba automática creó el lote DEMO-RAILWAY-AUTO-1794087974641, id 14. Sin invocar el endpoint de prueba, Resend lo aceptó en un intento a las 16:46:55 de Lima. ACCEPTED acredita la aceptación del proveedor; la recepción en bandeja se confirmó para el primer correo de prueba.

Figuras 4 y 5. Capturas reales del aviso móvil y del estado de correo. Fuentes: alerts-email-results.json, railway-email-test.json, railway-email-activation.json, railway-automatic-email.json y final-email-status-results.json.

## Persistencia de datos y comprobación del respaldo

Se reiniciaron realmente MySQL y la API entre las 16:41:00 y 16:41:31 de Lima. La API volvió a estado UP y la sesión continuó operativa. Antes y después se consultaron todas las páginas de productos, lotes y movimientos del visor administrativo, además del historial de correo, y se compararon conteos y hashes SHA256.

| Conjunto observado | Antes | Después | SHA256 coincide |
| --- | --- | --- | --- |
| productos | 15 | 15 | Sí |
| lotes | 13 | 13 | Sí |
| movimientos | 14 | 14 | Sí |
| avisos_correo (campos del historial) | 1 | 1 | Sí |

| Conjunto | Inicio del SHA256 antes y después |
| --- | --- |
| productos | 16d5d5cabf66b1a0c7313595… |
| lotes | 1be7c6c667719d3d896c7b1b… |
| movimientos | 1511e5a733c5a58d6aa8dc43… |
| avisos_correo (campos del historial) | f4c9fdcc7be4d9e655fb73c2… |

Resultado: los registros comparados se conservaron. El hash cubre los campos expuestos por el visor, no todas las columnas internas ni todas las tablas. El historial admite hasta 50 filas y contenía una al medir. Los conteos son anteriores al reconocimiento BASELINE y a la creación del lote automático; no representan el total actual.

El respaldo previo a migraciones se restauró en una base aislada y sus 20 tablas físicas conservaron los conteos. La fuente era MySQL 9.7.4 de Railway; la restauración se ensayó en MySQL local 8.4.8, ajustando únicamente los calificadores de esquema de las vistas. Se comprobó que las vistas no apuntaran a otra base y se eliminó la base temporal al terminar.

Esta comprobación no equivale a un ensayo de recuperación completo en Railway ni garantiza compatibilidad ante cualquier cambio de versión. El SQL, las contraseñas y las claves se guardan en archivos privados y no se adjuntan al informe.

Fuentes verificables: docs/deployment-validation/railway-persistence.json y railway-backup-check.json. Los hashes completos y conteos originales permanecen en esos archivos del repositorio.

## Regresión y alcance de la validación

| Archivo en docs/deployment-validation | Casos aprobados |
| --- | --- |
| flow-results.json | 1 recorrido principal |
| permissions-errors-results.json | 2 casos de permisos y errores |
| alerts-email-results.json | 2 casos de alertas y correo |
| security-results.json | 1 caso de tokens y límite de acceso |
| final-email-status-results.json | 1 comprobación adicional del correo activado |

Los seis casos principales comprobaron funciones anteriores junto con las nuevas alertas. Se aprobaron además la revisión del correo ya activado. La clave de GPT quedó configurada, pero la consulta externa no se considera validada en este informe.

La prueba de GPT no aprobó: OpenAI devolvió HTTP 401 con código expired_secret_key. La conexión WebSocket del chatbot se corrigió y un origen ajeno siguió siendo rechazado con 403; para validar las respuestas del modelo se requiere reemplazar la clave vencida y repetir el caso. El intento fallido no modificó el inventario.

Antes del despliegue se aprobaron 36 pruebas Java/MySQL y 11 del frontend, junto con la comprobación TypeScript local. GitHub Actions está pausado: no se presenta una ejecución de integración continua como aprobada. Las mediciones de carga del punto 22 fueron locales; no se ha medido una capacidad máxima de Railway.

La protección del último administrador se comprobó en las pruebas Java/MySQL aisladas. En la base compartida se omitió expresamente esa modificación para conservar las cuentas anteriores. El rol SUPERVISOR está definido en el código, pero no tuvo un recorrido público independiente durante esta validación.

## Defectos conocidos y condiciones pendientes

| Condición | Tratamiento o limitación |
| --- | --- |
| HTTP 429 en el pico local | Se mantiene el límite de entrada; revisar antes de ajustar. Falta repetir la carga en Railway. |
| Remitente Resend de demostración | Solo destinatario autorizado de la cuenta; otros correos requieren dominio verificado. |
| Avisos de dependencias | El push informó 31 alertas Dependabot de la rama predeterminada: 11 altas, 16 moderadas y 4 bajas. Evaluar aplicabilidad y corregir. |
| Restauración con motor distinto | Ensayar en un entorno aislado con la misma versión MySQL de Railway. |
| Clave GPT vencida | OpenAI: HTTP 401 / expired_secret_key. Reemplazar la clave privada y repetir la prueba. |

## Procedimiento para regresar a una versión estable

La referencia validada para volver después de cambios futuros es v1.1.0, commit 5cb4fbf2e841debd39938bd7d5a5234041f0615e. Los identificadores concretos de web, API, chatbot y MySQL se registran en railway-services.json. El responsable operativo es Stiven Bravo Malca.

1. Registrar el defecto, la versión y el momento. Limitar las operaciones si existe riesgo para los datos. Si el defecto afecta al correo, establecer ALERT_EMAIL_ENABLED=false y desplegar la configuración.

2. Revisar el historial de Railway. Si conserva un despliegue exitoso de la referencia estable, usar Rollback y comprobar las variables que recupera. Si no está retenido, desplegar de nuevo el SHA estable con la configuración privada guardada en los componentes afectados.

3. Conservar el volumen de MySQL. Restaurar una imagen anterior no restaura datos. V8 añade una tabla: avisos_correo puede mantenerse si el código anterior no la utiliza. No ejecutar una migración destructiva para revertir solo el código.

4. Si se necesita recuperar datos, poner el sistema en mantenimiento y respaldar primero el estado actual. Restaurar la copia elegida en una base independiente de la misma versión de MySQL; verificar esquema, tablas, vistas y flujo principal antes de cambiar la conexión. No sobrescribir directamente la base activa.

5. Advertir qué operaciones posteriores al respaldo se perderían si esa copia pasa a ser el estado definitivo. El responsable debe aceptar ese punto de recuperación antes de sustituir la conexión.

6. Comprobar salud de servicios, acceso ADMIN y WORKER, catálogo, lotes, atención, reportes, persistencia y manejo de errores. Revisar el historial antes de reactivar el correo para evitar duplicados.

7. Registrar responsable, versión recuperada, duración, pruebas y resultado; conservar las evidencias y el respaldo anterior hasta cerrar la incidencia.

Se verificaron el reinicio con persistencia y una restauración aislada del respaldo. No se ejecutó una reversión completa del despliegue productivo para elaborar este informe.

## Comprobaciones después de desplegar o revertir

La web debe devolver HTTP 200 en /login; la API debe informar UP en /actuator/health; /chat/_stcore/health debe responder 200. Una consulta protegida sin sesión debe seguir devolviendo 401. Después se repite el flujo con datos ficticios y se verifica el historial de operaciones.

## Control del entorno de demostración

Se configuró un límite de cómputo de US$10, aviso de US$5 y límite de Agent de US$0. La renovación mensual de Hobby permanece activa, con el ciclo observado hasta el 7 de diciembre de 2026. El límite no cancela la suscripción ni garantiza una factura total de US$5. Revisar Usage y Billing, detener los servicios al terminar las demostraciones y conservar el respaldo.

Las consultas del asistente usan una clave privada de OpenAI y generan consumo separado de Railway. n8n no se desplegó: la automatización de correo se ejecuta directamente en la API.

Figura 6. Captura de los controles de consumo configurados en Railway. Estos controles deben revisarse antes de cada demostración; no constituyen cancelación de la renovación.

## Trazabilidad y referencias

Repositorio: https://github.com/Daniel-ux1026/semaforo-digital-mass. Etiqueta: v1.1.0. Documento técnico actualizado: docs/despliegue-railway.md. Resultados originales: docs/deployment-validation. Las capturas de este Word proceden de las pruebas reales y de Railway; no son maquetas.

Referencias oficiales: docs.railway.com/deployments/monorepo; docs.railway.com/deployments/deployment-actions; docs.railway.com/networking/outbound-networking; resend.com/docs/api-reference/emails/send-email; developers.openai.com/api/docs/quickstart.
