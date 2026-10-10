# Base de datos en español

La base operativa es **MySQL**, en Docker. Esta carpeta contiene su estructura y una copia de consulta; la aplicación no guarda la base activa en un archivo SQL.

## Ver datos actuales

1. Inicia sesión como programador (administrador) en http://localhost:4200/login.
2. Se abrirá **/administracion**. Selecciona **Base de datos**.
3. Elige una tabla; sus columnas se presentan en español y la consulta tiene paginación.

## Archivos

- `estructura_mysql.sql`: definición real de las 19 tablas de negocio y la tabla técnica `flyway_schema_history`, sin filas. Incluye tipos, claves, índices, restricciones, 15 vistas de compatibilidad y los triggers que mantienen los perfiles. Las tablas de negocio tienen nombres españoles; las columnas internas mantienen compatibilidad con Java. La tabla técnica vacía sirve para visualizar su estructura, no para reemplazar el historial de una instalación.
- `insercion_datos_demo.sql`: datos ficticios para una instalación local ya iniciada: tres productos, tres lotes con distintos vencimientos, sus movimientos de ingreso y un evento de auditoría. No incluye cuentas ni claves. La carga usa una transacción y no duplica ni reinicia los lotes al repetirla.
- `datos/semaforo_consulta.sql`: copia de los datos visibles para administración, con tablas y columnas en español. Al ejecutarla en MySQL Workbench crea una base independiente con fecha en el nombre. Sus columnas son de texto para facilitar la consulta; no es un respaldo para restaurar la aplicación.
- `datos/datos.json`: la misma copia, legible en un editor.
- `inventario_exportacion.json`: fecha de exportación y cantidad de filas por tabla.
- `exportar-estructura.mjs`: genera la estructura desde la base MySQL activa, sin registros.
- `exportar.mjs`: actualiza la estructura, el SQL de consulta, el JSON y el inventario mediante el acceso administrativo.

Las copias no se actualizan automáticamente. Comprueba la fecha de `inventario_exportacion.json` y vuelve a exportar después de cambiar los datos. La fecha se guarda en UTC; para la hora de Perú se restan cinco horas.

La copia de consulta incluye las 19 tablas de negocio, entre ellas `empleado`, `supervisor`, `programador` y `solicitudes_clave`. El historial técnico de Flyway se incluye solamente en la estructura. El SQL de consulta sirve para ver datos; no contiene credenciales con las que iniciar sesión ni sustituye la base operativa.

Actualizar solamente la estructura, con Docker y el proyecto iniciados:

```powershell
node base_de_datos/exportar-estructura.mjs
```

Para visualizar este SQL en Workbench, crea una base vacía independiente, selecciónala con `USE nombre_de_tu_base;` y ejecuta el archivo. Los nombres ingleses corresponden a vistas de compatibilidad. El archivo no contiene cuentas ni contraseñas y no agrega registros.

Para instalar o actualizar la aplicación, ejecuta `INICIAR_PROYECTO.bat`: Flyway aplica las migraciones V1 a V6. No importes esta copia de estructura en la base operativa; una estructura ya creada con un historial Flyway vacío no sustituye la ejecución de las migraciones.

## Instalar desde GitHub y cargar los ejemplos

1. Descarga o clona el proyecto y ejecuta `INICIAR_PROYECTO.bat`, con Docker Desktop abierto. Las migraciones crean las tablas y la aplicación crea la cuenta inicial en una base vacía.
2. Consulta **BOOTSTRAP_DNI** y **BOOTSTRAP_PASSWORD** en tu archivo privado `infra/.env`. Entra como programador, cambia su clave inicial y crea las cuentas del supervisor y del empleado desde **Personal**. Cada instalación genera sus propias credenciales; las cuentas de otra computadora no se copian desde GitHub.
3. Para cargar inventario ficticio, detén temporalmente la API: desde la raíz ejecuta `docker compose --env-file infra/.env -f infra/compose.yml --profile full stop api`. MySQL permanece iniciado.
4. Conéctate con MySQL Workbench a **127.0.0.1**, puerto **3307**, usuario **root**, usando **MYSQL_ROOT_PASSWORD** de tu archivo privado `infra/.env`. El usuario de ejecución de la aplicación conserva sus privilegios limitados.
5. Abre `insercion_datos_demo.sql` y ejecuta el archivo completo. Se cargan tres lotes con 70, 20 y 5 días hasta el vencimiento: verde, amarillo y rojo inicialmente. Las fechas avanzan con el tiempo; repetir el SQL conserva los lotes ya cargados.
6. Inicia nuevamente la API: `docker compose --env-file infra/.env -f infra/compose.yml --profile full start api`. Recarga la interfaz y comprueba **Productos**, **Lotes** y **Resumen**. La aplicación genera las alertas al evaluar los lotes.

La carga requiere un programador activo y la estructura actual. Si encuentra un SKU de ejemplo ocupado por otro producto, rechaza la carga y revierte sus cambios. Si aparece ese error, ejecuta `DROP PROCEDURE IF EXISTS cargar_demo_mass_v1;` antes de reintentar. El procedimiento se elimina automáticamente después de una carga correcta.

Para estudiar únicamente las tablas, puedes importar `estructura_mysql.sql` en una base vacía distinta. Ese archivo no necesita conectarse a la aplicación. Para ejecutar el sistema, sigue la instalación mediante las migraciones.

Actualizar también la copia de consulta desde la raíz del proyecto:

```powershell
node base_de_datos/exportar.mjs
```

Usa la credencial del programador guardada localmente en `.local/demo-user.json`. Si cambias esa contraseña, actualiza ese archivo privado o consulta los datos desde la página. La carpeta `datos/` queda excluida de Git porque puede contener DNI, correo e información del inventario. No se exportan contraseñas, hashes de contraseña ni tokens. GitHub incluye la estructura y el script de datos ficticios; las exportaciones locales de consulta permanecen privadas.

## Nombres físicos

usuarios, empleado, supervisor, programador, solicitudes_clave, productos, lotes, movimientos, alertas, auditoria, evaluaciones, notificaciones, sesiones, renovaciones_sesion, recuperaciones_clave, sesiones_chat, operaciones_idempotentes, recepciones_servicio y bloqueo_tienda.

Flyway conserva su tabla técnica `flyway_schema_history`. Hay vistas de compatibilidad con nombres ingleses para que las consultas existentes de la API sigan funcionando; no son copias adicionales de los datos. La migración V4 renombra las tablas sin borrar registros. Los cambios operativos deben realizarse desde sus módulos para mantener validaciones y auditoría.
