# Avisos de vencimiento

Implementación local del 7 de noviembre de 2026, sobre la actualización del repositorio 9461354. No se ha publicado esta modificación.

## Comportamiento

- Consulta los lotes críticos (ROJO) y vencidos (VENCIDO), con stock y producto activo, usando la clasificación del servidor.
- Recorre todas las páginas, sin depender del filtro de búsqueda ni de los primeros resultados del panel.
- Presenta producto, código de lote, fecha, plazo y saldo. Muestra hasta tres lotes y el número restante; **Ver alertas** abre el listado completo.
- Revisa al cargar o actualizar el apartado y cada 45 segundos si la pestaña está visible y conectada.
- Cerrar el aviso lo reconoce durante la sesión de página. No se repite mientras el lote conserva el mismo estado. Pasar de ROJO a VENCIDO genera otro aviso; recargar la página reinicia los reconocimientos.
- El aviso no toma el foco y se oculta mientras están abiertos el menú, los diálogos de edición o el panel de notificaciones.
- El sonido se habilita con el botón de la barra superior y se puede silenciar. Al activarlo se reproduce una señal de confirmación.

## Validación

Resultado local: 11 pruebas unitarias aprobadas tanto en DOM como en Chromium; compilación y comprobación de tipos correctas; 5 pruebas de recorrido aprobadas en Edge (27,4 segundos). La suite incluye el nuevo aviso y las comprobaciones anteriores de inventario, permisos, sesiones, escáner y errores. Estos tiempos son de ejecución de pruebas, no una medición de capacidad del sistema.

Las pruebas unitarias verifican paginación, reconocimiento sin repetición, transición a vencido, exclusión de lotes agotados o productos inactivos y conservación del aviso ante una consulta fallida.

La prueba de navegador `e2e/alerts-expiry.spec.ts` registra un producto y un lote ficticios mediante la API local, con vencimiento al día siguiente. Comprueba que aparece el código del lote y «Vence mañana», que Web Audio genera dos tonos y que el aviso cerrado no reaparece al actualizar. También verifica la vista de 390 × 844 y ausencia de errores JavaScript.

Evidencias locales: `docs/evidence/expiry-alert-desktop.png` y `docs/evidence/expiry-alert-mobile.png`. Estas capturas usan inventario ficticio y no representan datos de una tienda real.

## Limitaciones

Requiere la aplicación abierta, conexión y la pestaña visible para la revisión periódica. No hay envíos externos ni notificaciones con el navegador cerrado. La detección periódica puede tardar hasta 45 segundos, además del tiempo de respuesta del servidor.

El navegador necesita una interacción para habilitar el sonido. Las pruebas automatizadas comprueban la generación de audio, pero el volumen y la salida física deben comprobarse en el equipo de demostración. No se han medido todavía los costos de estas consultas con una cantidad grande de lotes; corresponde evaluarlos en el punto de capacidad y rendimiento.
