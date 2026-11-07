# Consulta de alertas críticas con k6

Operación: GET /api/alerts?state=ROJO&page=0. Prueba de lectura con cuentas WORKER y datos ficticios. No ejecuta ventas, movimientos ni envíos de correo.

Configuración ejecutada el 7 de noviembre de 2026:

| Escenario | VUS | Tiempo | Pausa |
|---|---:|---:|---:|
| Línea base | 1 | 45 s | 1 s |
| Normal | 3 | 45 s | 1 s |
| Pico | 10 | 45 s | 1 s |

Las sesiones se obtienen fuera de la medición y se guardan en un arreglo de tokens dentro de un archivo privado. Cada usuario virtual utiliza una cuenta distinta. Para repetir se deben preparar nuevas sesiones; las diez cuentas del experimento fueron desactivadas después de medir.

La imagen utilizada fue grafana/k6 2.3.0, digest sha256:3e4186aea62e11a45560bfc51e19eeafe82fc07ce069c78aff44b7ff893d9a5e.

Ejemplo de ejecución sobre el ambiente Docker local:

```powershell
$projectRoot=(Get-Location).Path
docker run --rm --network semaforo-final-1107_default --mount "type=bind,source=$projectRoot/tests/performance,target=/scripts,readonly" --mount "type=bind,source=$projectRoot/.local/performance,target=/private,readonly" --mount "type=bind,source=$projectRoot/docs/performance-2026-11-07,target=/results" -e VUS=3 -e RUN_NAME=normal grafana/k6@sha256:3e4186aea62e11a45560bfc51e19eeafe82fc07ce069c78aff44b7ff893d9a5e run --no-color /scripts/alerts.k6.js
```

El archivo privado /private/tokens.json debe contener únicamente el arreglo de tokens vigentes. No debe versionarse. El destino web corresponde al Nginx de este proyecto Compose. Al repetir, elegir un nombre de ejecución distinto para conservar las evidencias anteriores.

Se hicieron diez consultas de calentamiento antes de medir. Cada escenario empezó en una ventana nueva del límite por minuto y no se ejecutaron otros recorridos de navegador durante la carga. Las capturas de la interfaz se tomaron al finalizar.

Las respuestas 429 cuentan como fallo de la operación. La latencia successful_duration recoge solo respuestas HTTP 200; no debe confundirse con disponibilidad. El código de salida 99 en el pico indica que se incumplieron umbrales, no un error de instalación de k6.

Los resultados originales, muestras de recursos, capturas y manifiesto de versión están en docs/performance-2026-11-07. Las capturas de métricas muestran los valores originales en un visor HTML y no simulan una consola nativa.

Los valores normales y pico son supuestos académicos; una sola ejecución local de 45 s con 95 lotes no demuestra la capacidad máxima de producción.
