# GitHub: ramas, integración, entrega y despliegue

Repositorio privado: Daniel-ux1026/semaforo-digital-mass. Commits en español. Usar dev para cambios de prueba, pull request a main y CI aprobado antes de integrar. main es la rama por defecto y está protegida: exige los cinco controles de CI, rechaza borrados y subidas forzadas, incluso para administradores. Conservar las ramas; no subir .env ni datos de MySQL.

## Integración continua

ci.yml se ejecuta con push a main/dev, pull requests y ejecución manual. Comprueba build y siete pruebas Angular con Chrome; tests Java/MySQL (Testcontainers), SpotBugs y construcción de las tres imágenes. También revisa archivos versionados por patrones de secretos. Acciones fijadas a SHA. Token de CI solo lectura; las pruebas no usan claves de OpenAI, SMTP ni acceso al servidor.

## Entrega

entrega-despliegue.yml espera CI aprobado para un push del propio repositorio en main o dev. Publica api, web y chatbot en ghcr.io/daniel-ux1026/semaforo-digital-mass con SHA completo y etiqueta de rama. La publicación usa GITHUB_TOKEN temporal con permiso packages:write. No publica imágenes desde pull requests ni forks.

## Estado del despliegue

Preparado para servidor Linux por SSH, desactivado por DEPLOY_ENABLED=false. No existe todavía un servidor configurado ni se enviaron credenciales reales a GitHub. Publicar imágenes no significa que la aplicación esté alojada en Internet. No usar GitHub Pages: requiere backend y MySQL.

El alojamiento requiere un servidor Linux propio, Docker Compose, Git, Node 24, dominio HTTPS y una instalación inicial con volúmenes, infra/.env privado, usuario migrador y credenciales de lectura para repo/GHCR. Los puertos quedan en localhost; un proxy TLS del servidor publica la web. Configurar APP_ORIGIN=https://tu-dominio y SECURE_COOKIE=true. Administrar n8n por acceso privado.

Cuando el servidor exista, ir a Settings > Environments > produccion. Variables: SSH_HOST, SSH_USER, SSH_PORT, DEPLOY_PATH. Secretos: SSH_PRIVATE_KEY, SSH_KNOWN_HOSTS, REGISTRY_USER, REGISTRY_TOKEN (solo read:packages para imágenes privadas). Verificar huella del servidor con su proveedor antes de guardar known_hosts; no copiar claves a este chat. Usar usuario SSH dedicado con acceso limitado al proyecto; acceso a Docker implica privilegios elevados.

DEPLOY_ENABLED es variable de repositorio. Mantener false mientras falte la configuración y cambiar a true solo al autorizar el servidor preparado. El entorno produccion acepta únicamente main. GitHub rechazó la protección por revisores obligatorios porque el plan actual no la admite en este repositorio privado. La protección de main sí está habilitada. Antes de activar producción, revisar si el plan permite añadir aprobaciones del entorno.

El despliegue solo usa main aprobado, verifica host SSH, descarga imágenes por SHA, respalda/verifica restauración de MySQL, conserva volúmenes, aplica migraciones con usuario separado y comprueba salud. No usa contraseñas en argumentos ni desactiva StrictHostKeyChecking. Las credenciales temporales del runner y Docker se eliminan al terminar.

Una migración de BD puede requerir recuperación manual; no se revierte automáticamente el esquema si falla la salud. Conservar respaldo anterior, SHA previo y procedimiento de restauración. Falta probar el despliegue contra un servidor real antes de considerarlo validado.

## Archivos publicados

Fuentes Angular/Java/Python, package-lock, Maven wrapper, migraciones, flujos n8n sin credenciales, Docker, scripts operativos, pruebas vigentes y documentación mínima. Se excluyen .local, infra/.env, respaldos, datos exportados, informes JSON, capturas, node_modules, target y dist. Se revisa además que los secretos de .env local no aparezcan en los archivos del primer commit.

## Fuentes

- https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images
- https://docs.github.com/en/actions/how-tos/deploy/configure-and-manage-deployments/manage-environments
