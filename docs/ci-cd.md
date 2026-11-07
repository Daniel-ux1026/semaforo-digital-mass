# Ramas, integración y despliegue

El repositorio público **Daniel-ux1026/semaforo-digital-mass** conserva las ramas **dev** y **main**. Los cambios se preparan en dev y se incorporan a main mediante una solicitud de cambios, con mensajes de commit en español.

## Estado actual

GitHub Actions está desactivado temporalmente desde la configuración del repositorio, a petición del responsable del proyecto. Los archivos de los workflows permanecen versionados para su reactivación.

Durante la pausa, las pruebas se ejecutan localmente. La construcción automática de imágenes, su publicación en GHCR y el despliegue automatizado quedan pausados junto con Actions.

La protección de main exige comprobaciones de CI. Mientras esas comprobaciones sigan configuradas y Actions esté desactivado, GitHub puede bloquear una incorporación a main. Cualquier excepción administrativa o modificación de esa política requiere autorización del responsable del repositorio.

## Integración continua preparada

[ci.yml](../.github/workflows/ci.yml) define ejecuciones para push a main o dev, pull requests y lanzamiento manual cuando Actions esté habilitado.

| Trabajo | Verificaciones |
| --- | --- |
| Frontend | Instalación reproducible, patrones de secretos, auditoría npm, compilación, pruebas Angular, revisión de tipos y ejecución en Chromium. |
| Backend | Pruebas Java, migraciones en MySQL con Testcontainers y análisis SpotBugs. |
| Contenedores | Construcción de las imágenes de API, web y chatbot. |

Las acciones utilizadas están fijadas por SHA y los permisos del workflow de CI se limitan a lectura del repositorio. Las pruebas de CI utilizan datos de prueba.

Los comandos locales se detallan en la sección [Pruebas del README](../README.md#pruebas). Los recorridos de Playwright modifican datos y deben ejecutarse sobre una instancia de pruebas.

## Entrega de imágenes

[entrega-despliegue.yml](../.github/workflows/entrega-despliegue.yml) espera una ejecución de CI aprobada para un push del propio repositorio en main o dev. El trabajo de entrega publica las imágenes de API, web y chatbot en GitHub Container Registry con una etiqueta de commit y otra de rama.

La publicación utiliza el token temporal de GitHub con permiso de escritura en paquetes. La generación de imágenes prepara artefactos para el despliegue.

## Preparación de producción

La configuración de despliegue está preparada para un servidor Linux con Docker por SSH. Su ejecución depende de **DEPLOY_ENABLED=true**, de una ejecución de CI aprobada en main y de la configuración del entorno **produccion**.

| Configuración | Valores previstos |
| --- | --- |
| Variables del entorno | SSH_HOST, SSH_USER, SSH_PORT, DEPLOY_PATH |
| Secretos del entorno | SSH_PRIVATE_KEY, SSH_KNOWN_HOSTS, REGISTRY_USER, REGISTRY_TOKEN |
| Configuración privada del servidor | infra/.env, origen HTTPS y cookies seguras |

El servidor debe disponer de Docker Compose, Git, Node.js, almacenamiento persistente para MySQL y acceso al registro de imágenes. La huella SSH se verifica antes de guardar SSH_KNOWN_HOSTS.

El arranque local y el despliegue preparan el migrador antes de iniciar la API. La migración V7 transfiere la ejecución de los disparadores de perfiles al migrador. Después de aplicar las migraciones se comprueba esa propiedad, se limitan los permisos de la cuenta de aplicación y se reinicia la API para abrir conexiones con esos permisos.

La validación local de V7 comprobó una instalación vacía y la recuperación de una instalación V6 cuyo administrador no se había creado. Pasaron las 32 pruebas Java/MySQL, SpotBugs y los cuatro recorridos Playwright. También se comprobaron el alta de personal, el cambio de empleado a supervisor y de vuelta a empleado con la cuenta de aplicación limitada. Tras reiniciar MySQL y la API se conservaron los 11 lotes y 12 movimientos de la prueba. Estas comprobaciones deben repetirse en la plataforma elegida antes de aceptar el despliegue público.

Los scripts descargan imágenes por commit, respaldan MySQL, comprueban una restauración y verifican la salud después de iniciar los servicios. Las migraciones pueden requerir recuperación manual del esquema y deben acompañarse de un respaldo anterior.

La puesta en producción todavía requiere validación contra el servidor real y su dirección pública.

## Reactivación de Actions

1. Resolver el estado de la cuenta que impedía iniciar los trabajos.
2. Revisar **Settings → Actions → General** y habilitar GitHub Actions.
3. Restaurar la política previa de acciones seleccionadas y de referencias fijadas por SHA.
4. Revisar las comprobaciones obligatorias de main y su correspondencia con los nombres de los trabajos.
5. Ejecutar CI sobre dev, comprobar sus resultados y después incorporar los cambios a main.
6. Comprobar por separado la publicación de imágenes y el estado de DEPLOY_ENABLED.

El despliegue se activa cuando su servidor y sus secretos estén configurados y validados.

## Material versionado

Se publican fuentes, dependencias declaradas, migraciones, SQL de demostración, pruebas, scripts y documentación. Las capturas del README contienen datos ficticios.

Los archivos **infra/.env**, **.local/**, los respaldos, las exportaciones privadas y los directorios de dependencias o compilación se excluyen mediante .gitignore. **scripts/check-tracked-secrets.mjs** revisa los archivos versionados.
