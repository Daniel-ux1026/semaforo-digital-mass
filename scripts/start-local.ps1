param([switch]$Full)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
if(-not(Test-Path -LiteralPath 'infra/.env')){& ./scripts/setup-local.ps1}
docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
if($LASTEXITCODE -ne 0){throw 'Inicie Docker Desktop y espere a que el motor esté disponible.'}
docker compose --env-file infra/.env -f infra/compose.yml --profile full up -d --wait mysql n8n
if($LASTEXITCODE -ne 0){throw 'No se pudo iniciar la infraestructura. Revise Docker.'}
node scripts/harden-db.mjs --prepare
if($LASTEXITCODE -ne 0){throw 'No se pudo preparar el usuario migrador de MySQL.'}
docker compose --env-file infra/.env -f infra/compose.yml --profile full up -d --build api
if($LASTEXITCODE -ne 0){throw 'No se pudo iniciar la API.'}
Write-Host 'Esperando migraciones de la API...'
$apiReady=$false
for($attempt=0;$attempt -lt 60;$attempt++){
 try { $health=Invoke-RestMethod 'http://localhost:8080/actuator/health' -TimeoutSec 3;if($health.status -eq 'UP'){$apiReady=$true;break} }catch{}
 Start-Sleep -Seconds 1
}
if(-not $apiReady){throw 'La API no respondió. Consulte docker compose logs api.'}
node scripts/harden-db.mjs
if($LASTEXITCODE -ne 0){throw 'No se pudieron separar los privilegios de MySQL.'}
docker compose --env-file infra/.env -f infra/compose.yml --profile full restart api
if($LASTEXITCODE -ne 0){throw 'No se pudo reiniciar la API con los permisos limitados.'}
if($Full){
 docker compose --env-file infra/.env -f infra/compose.yml --profile full up -d --build web chatbot
 if($LASTEXITCODE -ne 0){throw 'No se pudo iniciar la web o el chatbot.'}
 $webReady=$false
 for($attempt=0;$attempt -lt 60;$attempt++){
  try { $health=Invoke-RestMethod 'http://localhost:8080/actuator/health' -TimeoutSec 3;$web=Invoke-WebRequest 'http://localhost:4200/login' -UseBasicParsing -TimeoutSec 3;if($health.status -eq 'UP' -and $web.StatusCode -eq 200){$webReady=$true;break} }catch{}
  Start-Sleep -Seconds 1
 }
 if(-not $webReady){throw 'La web/API no respondió. Revise los servicios de Docker.'}
 Write-Host 'Aplicación: http://localhost:4200'
}
else{if(-not(Test-Path node_modules)){npm.cmd ci --ignore-scripts};Write-Host 'Aplicación: http://localhost:4200 (Ctrl+C detiene Angular)';npm.cmd start}

