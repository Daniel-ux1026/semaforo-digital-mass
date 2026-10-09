param([string]$Dni)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$envPath = Join-Path $projectRoot 'infra/.env'
if (Test-Path -LiteralPath $envPath) { Write-Host 'infra/.env ya existe; no se sobrescribió.'; exit 0 }
if (-not $Dni) { $Dni = (Get-Random -Minimum 10000000 -Maximum 99999999).ToString() }
if ($Dni -notmatch '^\d{8}$') { throw 'DNI debe tener ocho dígitos.' }
function New-Secret {
 $bytes = New-Object byte[] 36
 $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
 try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
 [Convert]::ToBase64String($bytes)
}
$bootstrapPassword = New-Secret
$values = [ordered]@{MYSQL_ROOT_PASSWORD=(New-Secret);DB_PASSWORD=(New-Secret);JWT_SECRET=(New-Secret);BOOTSTRAP_DNI=$Dni;BOOTSTRAP_PASSWORD=$bootstrapPassword;N8N_ENCRYPTION_KEY=(New-Secret);N8N_TOKEN=(New-Secret);SERVICE_TOKEN=(New-Secret)}
$lines = foreach($entry in $values.GetEnumerator()) { "$($entry.Key)=$($entry.Value)" }
[IO.File]::WriteAllLines($envPath,$lines)
Write-Host 'Configuración generada en infra/.env. Consulte allí BOOTSTRAP_DNI y BOOTSTRAP_PASSWORD. No comparta este archivo.'
