# Contraseñas, notificaciones y chatbot

## Solicitudes de contraseña

Solo el programador puede asignar y restablecer contraseñas de empleados y supervisores. El personal ingresa con la clave asignada y solicita su cambio desde **Mi cuenta**, con una sesión activa.

La solicitud se guarda en **solicitudes_clave**. Cada cuenta puede tener una solicitud pendiente. El programador la consulta en la pestaña **Contraseñas** de la campana y utiliza el formulario de restablecimiento para asignar la nueva clave.

Al guardar, la API almacena la contraseña con BCrypt, resuelve la solicitud, registra la operación en auditoría y revoca las sesiones anteriores de esa cuenta. El programador comunica la nueva clave mediante un canal privado acordado.

El panel consulta las solicitudes cada tres segundos mientras la página está visible y mantiene su paginación. La autorización también se aplica en el servidor. El empleado y el supervisor tienen restringidos el cambio de clave propio y la recuperación por correo.

La recuperación por SMTP corresponde al programador y requiere configurar el correo. El inicio de sesión muestra el formulario de acceso y el logo de Mass. Una persona que haya perdido el acceso debe solicitar el restablecimiento al programador por un canal externo.

## Notificaciones de vencimiento

La campana está disponible para los tres roles. Muestra los recuentos de lotes vencidos, críticos y próximos a vencer, junto con una selección de lotes para revisar.

Al seleccionar un estado se abre el listado correspondiente. Al seleccionar un lote se aplica su código como búsqueda. La pestaña de solicitudes de contraseña es exclusiva del programador.

El panel consulta los vencimientos al abrirse y se actualiza cada 45 segundos mientras la página está visible. Esta actualización utiliza consultas periódicas a la API.

## Asistente

Los tres roles pueden abrir una sesión del asistente desde su panel. La API valida la cuenta, la sesión y la caducidad del acceso antes de entregar el contexto a Streamlit.

El contexto se limita al inventario permitido por rol. Empleados y supervisores reciben cantidades y alertas sin costos ni pérdidas monetarias. Los datos de cuentas y las solicitudes de contraseña quedan fuera del contexto de GPT.

El asistente realiza consultas de apoyo. Las operaciones de inventario se registran desde los formularios de la aplicación. Las consultas locales de n8n están reservadas al programador.

La conexión a OpenAI requiere **OPENAI_API_KEY**, un modelo disponible y cuota de API. Las pruebas y las capturas de la documentación utilizan datos ficticios.
