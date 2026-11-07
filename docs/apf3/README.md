# Avance 03 de Semáforo Digital Mass

## Presentación ejecutiva

Semáforo Digital Mass ayuda al personal de una tienda a identificar los lotes que vencen primero. Registramos el producto, la fecha y las unidades; el panel indica qué revisar y el historial conserva las promociones, ventas y mermas. La meta es reducir revisiones repetidas y errores. Aún no medimos ahorro ni reducción de mermas en una tienda real.

## Alcance funcional

Probamos en local el árbol del commit [58b31d7](https://github.com/Daniel-ux1026/semaforo-digital-mass/tree/58b31d761af51b0849a840e93859e329228aa654), idéntico al del commit de revisión `757b104`. El navegador usa Angular, Nginx reenvía las peticiones al API de Spring Boot y MySQL conserva los datos. Flyway aplica las seis migraciones.

| Historia | Resultado comprobado o alcance |
| --- | --- |
| HU-01 Acceso | Inicio de sesión local y rechazo sin sesión en personal. |
| HU-02 Roles | Empleado consulta y registra acciones permitidas; supervisor revisa catálogo y reportes; programador administra cuentas y claves. |
| HU-03 Código de barras | El EAN-13 localiza el producto. Se probaron el código inválido, el desconocido y el permiso de cámara denegado. Falta la prueba con equipo físico. |
| HU-04 Alertas | El panel muestra los estados y el orden de vencimiento. Su cálculo funciona sin n8n. |
| HU-05 Acciones del lote | El ingreso y la promoción se registraron desde la interfaz. MySQL conservó los saldos y el historial. |
| HU-06 Asistente | La arquitectura contempla el asistente. La integración externa y los avisos no forman parte de esta comprobación. |

En la demostración registramos 20 unidades de un lote ficticio y trasladamos 5 a promoción. MySQL conservó 15 normales y 5 en promoción, incluso tras reiniciar la aplicación y la base. Una venta superior al saldo respondió HTTP 400 sin modificar los datos.

Para iniciar una instalación propia, abrir Docker Desktop, ejecutar `INICIAR_PROYECTO.bat` y acceder a `http://localhost:4200/login`. Cada instalación genera su configuración y sus cuentas.

## Gestión de solicitudes

Usamos una bitácora de eventos con identificador, fecha, estado, responsable y detalle. El procedimiento consiste en registrar, validar, aprobar cuando corresponde, asignar, atender, verificar y cerrar.

| Caso | Resultado |
| --- | --- |
| SI-001 Información | Respuesta escrita sobre los estados del semáforo y el registro de mermas. Conformidad y cierre simulados. |
| SR-002 Alta de acceso | Aprobación simulada. Alta y vínculo con supervisor comprobados en MySQL. Consulta de lotes 200; gestión de personal y edición de catálogo 403. |
| SR-003 Alta incompleta | Pendiente por falta de datos y autorización. No se envió el alta y el conteo de cuentas se conservó. |

Los solicitantes son ficticios. El alta y la comprobación de permisos se ejecutaron en el sistema local.

## Control del cambio CH-01

Agregamos la estructura de referencia y el [script de datos de demostración](../../base_de_datos/insercion_datos_demo.sql) para preparar ejemplos en otra instalación sin copiar una base privada. Actualizamos el [README](../../README.md) y la [guía de base de datos](../../base_de_datos/LEEME.md). Las migraciones no cambiaron.

- [PR 6 y archivos afectados](https://github.com/Daniel-ux1026/semaforo-digital-mass/pull/6/files).
- [GitHub Actions](https://github.com/Daniel-ux1026/semaforo-digital-mass/actions/runs/38022483322): cinco controles aprobados en el commit de revisión.
- Prueba local: primera ejecución, tres lotes nuevos; segunda ejecución, cero duplicados.
- Reversión: dejar de ejecutar el script y usar la versión anterior compatible. Si se necesita revertir los registros, restaurar la copia de prueba o usar un volumen nuevo. Revertir el código por sí solo no elimina filas.

Los entregables académicos completos y las capturas se conservan en la carpeta local del equipo.
