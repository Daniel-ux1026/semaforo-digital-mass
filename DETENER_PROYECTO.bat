@echo off
setlocal
title Detener Semaforo Digital Mass
cd /d "%~dp0"
echo Deteniendo web, API, chatbot, n8n y MySQL...
where docker >nul 2>&1
if errorlevel 1 goto error
if not exist "infra\.env" goto sin_configuracion
docker compose --env-file "infra/.env" -f "infra/compose.yml" --profile full stop --timeout 30
if errorlevel 1 goto error
echo.
echo Proyecto detenido. La base de datos y los archivos se conservan.
echo Para volver a iniciar, abra INICIAR_PROYECTO.bat.
pause
exit /b 0
:sin_configuracion
echo ERROR: No se encontro infra\.env. Mantenga este archivo dentro del proyecto.
goto fin_error
:error
echo ERROR: No se pudo detener el proyecto. Compruebe que Docker Desktop este abierto.
:fin_error
pause
exit /b 1
