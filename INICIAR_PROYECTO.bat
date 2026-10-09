@echo off
setlocal
title Iniciar Semaforo Digital Mass
cd /d "%~dp0"
echo Iniciando Semaforo Digital Mass...
echo Mantenga Docker Desktop abierto. El primer inicio puede tardar varios minutos.
where docker >nul 2>&1
if errorlevel 1 goto falta_docker
where node >nul 2>&1
if errorlevel 1 goto falta_node
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-local.ps1" -Full
if errorlevel 1 goto error
echo.
echo Proyecto iniciado: http://localhost:4200
echo Automatizacion n8n: http://localhost:5678
start "" "http://localhost:4200"
echo Puede cerrar esta ventana; los servicios continuaran funcionando.
pause
exit /b 0
:falta_docker
echo ERROR: Docker no esta instalado o no esta disponible en PATH.
goto error
:falta_node
echo ERROR: Node.js no esta instalado o no esta disponible en PATH.
:error
echo.
echo No se pudo completar el inicio. Revise el mensaje anterior.
echo Abra Docker Desktop y espere a que su motor este listo antes de reintentar.
pause
exit /b 1
