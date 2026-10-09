$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Get-Content -LiteralPath (Join-Path $projectRoot 'infra/.env') | ForEach-Object {
 if ($_ -match '^([A-Z_]+)=(.*)$') { [Environment]::SetEnvironmentVariable($Matches[1],$Matches[2],'Process') }
}
Set-Location (Join-Path $projectRoot 'backend')
& ./mvnw.cmd spring-boot:run
