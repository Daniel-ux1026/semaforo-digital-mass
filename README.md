# Semáforo Digital Mass

Sistema académico de inventario y vencimientos para una tienda. Angular, Spring Boot Java 21, MySQL, Streamlit y n8n; servicios locales mediante Docker Compose.

## Roles

- **Programador (administrador):** administra cuentas, asignaciones, costos, auditoría y automatización; realiza bajas lógicas y cambia contraseñas.
- **Supervisor:** registra operaciones, crea/edita productos y plazos, corrige movimientos y consulta reportes.
- **Empleado:** consulta inventario y registra ingresos, ventas, promociones y mermas; no edita ni elimina.

Las cuentas comparten identidad en usuarios y tienen perfiles físicos empleado, supervisor y programador. Empleados y supervisores solicitan cambio de clave desde Mi cuenta; el programador recibe solicitudes con actualización cada tres segundos. Las contraseñas se guardan como hash BCrypt. El chatbot es de consulta para los tres roles; el personal no recibe costos ni pérdidas monetarias.

## Inicio local en Windows

Requisitos: Docker Desktop con motor Linux y Node.js 24. Ejecutar INICIAR_PROYECTO.bat; el primer inicio genera infra/.env con credenciales aleatorias y aplica migraciones. Ejecutar DETENER_PROYECTO.bat para detener conservando volúmenes.

- Acceso: http://localhost:4200/login
- Programador: http://localhost:4200/administracion
- Empleado/supervisor: http://localhost:4200/
- MySQL: 127.0.0.1:3307, base semaforo; las credenciales están solo en infra/.env.

En una base vacía, BOOTSTRAP_DNI y BOOTSTRAP_PASSWORD crean el programador inicial. El programador cambia su clave inicial y crea al personal. No hay cuentas reales ni datos de inventario incluidos en este repositorio.

## Configuración privada

infra/.env.example enumera las variables. Guardar OpenAI y SMTP en infra/.env, nunca en Angular o Git. GPT necesita una clave y cuota API; WhatsApp todavía requiere configurar un proveedor. La recuperación por correo es exclusiva del programador. La solicitud simulada del login conserva el comportamiento académico acordado.

Las migraciones backend/src/main/resources/db/migration son la fuente de estructura de BD. base_de_datos/exportar.mjs permite generar una copia privada de consulta; no se publica el contenido.

## Validación

Frontend: npm ci --ignore-scripts; npm run build; npm test -- --watch=false --browsers=ChromeHeadless.

Backend: cd backend y ejecutar bash mvnw test en Linux o mvnw.cmd test en Windows. Docker debe estar disponible para MySQL de Testcontainers. Las pruebas usan bases temporales; no necesitan credenciales reales.

## Ramas y automatización

main contiene la versión aprobada. Los cambios se trabajan en dev y se proponen mediante pull request a main, con commits en español. GitHub Actions comprueba frontend, backend y construcción Docker; publica imágenes verificadas en GHCR y prepara despliegue por SSH.

Consultar docs/ci-cd.md para ramas, controles y configuración de servidor. El despliegue remoto está desactivado hasta preparar servidor, HTTPS y secretos del entorno produccion. No se despliega la rama dev a producción.

## Alcance

Inventario manual para una tienda, sin POS/ERP. No se incluyen secretos, respaldos, exportaciones de datos, compilados, dependencias instaladas ni evidencias locales. No se afirma entrega real por WhatsApp ni cobertura de pruebas físicas de cámara.
