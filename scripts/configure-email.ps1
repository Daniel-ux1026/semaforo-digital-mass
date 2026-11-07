param([string]$Address = '')
$ErrorActionPreference = 'Stop'
if([string]::IsNullOrWhiteSpace($Address)){$Address=Read-Host 'Gmail del remitente y destinatario'}
$projectRoot = Split-Path $PSScriptRoot -Parent
$privateDirectory = Join-Path $projectRoot '.local'
New-Item -ItemType Directory -Path $privateDirectory -Force | Out-Null
if ($Address -notmatch '^[a-zA-Z0-9._%+-]+@gmail\.com$') { throw 'Indica una dirección Gmail válida.' }
Write-Host 'Ingresa una NUEVA contraseña de aplicación de Google. No se mostrará en pantalla.'
$secret = Read-Host 'Contraseña de aplicación' -AsSecureString
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
try {
 $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) -replace '\s',''
 if ($password -notmatch '^[a-zA-Z]{16}$') { throw 'Se esperaban las 16 letras de una contraseña de aplicación de Google.' }
 $lines = @('SMTP_HOST=smtp.gmail.com','SMTP_PORT=587',"SMTP_USERNAME=$Address","SMTP_PASSWORD=$password","SMTP_FROM=$Address",'SMTP_SSL=false','SMTP_STARTTLS=true','ALERT_EMAIL_ENABLED=false',"ALERT_EMAIL_TO=$Address")
 [IO.File]::WriteAllLines((Join-Path $privateDirectory 'email.env'),$lines,[Text.Encoding]::ASCII)
 Write-Host 'Configuración guardada en .local/email.env, excluida de Git. Envío automático todavía desactivado.'
} finally {
 [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
 $password = $null
 $secret.Dispose()
}
