# Contraseñas y chatbot — política vigente del 09/10/2026

Solo el programador administrador puede asignar y restablecer contraseñas de empleados y supervisores. Las nuevas cuentas y los restablecimientos permiten ingresar directamente; no exigen cambio propio. V6 elimina la obligación pendiente en las cuentas de personal existentes sin cambiar su contraseña.

En Mi cuenta, el personal ve: «Para restablecer contraseña debe enviar una solicitud al programador», con un botón para enviarla. Se guarda en solicitudes_clave, con una sola solicitud pendiente por cuenta. El programador ve un botón de campana con contador; consulta automáticamente cada tres segundos. Es actualización mediante polling, no WebSocket. El panel pagina las solicitudes y abre el formulario enmascarado para asignar la nueva clave. Guardar la clave BCrypt en BD resuelve la solicitud, registra auditoría y revoca las sesiones previas. El programador debe comunicar la clave al usuario por un canal privado acordado; las notificaciones no contienen contraseñas.

El bloqueo también se aplica a POST /auth/password y a recuperación por correo: empleados y supervisores no pueden eludir el flujo desde API o /restablecer. El programador conserva cambio propio y recuperación por correo si configura SMTP. El enlace simulado del login conserva su comportamiento.

El chatbot está disponible para los tres roles mediante /chat/session. El ticket valida sesión activa, usuario y expiración; la salida no modifica datos. Empleados y supervisores reciben alertas y cantidades de merma sin costos ni pérdidas monetarias. No se entregan datos de cuentas o solicitudes a GPT. Las consultas locales antiguas de n8n siguen siendo solo del programador.

Pruebas: 31 pruebas backend aprobadas, siete Angular aprobadas y recorrido real de empleado/supervisor con solicitud duplicada controlada, aparición automática en panel, restablecimiento guardado, rechazo de clave anterior, revocación de sesión, ingreso con la asignada y disponibilidad de Streamlit. No se enviaron consultas pagadas a GPT. Dos cuentas temporales desactivadas al finalizar. Evidencia: password-policy-verification.json y password-policy-java.log. El visor ahora incluye 19 tablas de negocio.

Las pruebas y guías anteriores que exigían cambio de clave temporal al personal quedan reemplazadas por esta política. Una persona que ya no puede iniciar sesión deberá pedir al programador el restablecimiento fuera del panel; este flujo de solicitud requiere una sesión activa.
