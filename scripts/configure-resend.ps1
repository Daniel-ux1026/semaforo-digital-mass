param([string]$Recipient='')
$ErrorActionPreference='Stop'
if([string]::IsNullOrWhiteSpace($Recipient)){$Recipient=Read-Host 'Correo destinatario verificado en Resend'}
$projectRoot=Split-Path $PSScriptRoot -Parent
if($Recipient -notmatch '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'){throw 'Destinatario inválido.'}
New-Item -ItemType Directory -Path (Join-Path $projectRoot '.local') -Force | Out-Null
$secret=Read-Host 'Clave API de Resend (se oculta al escribir)' -AsSecureString
$pointer=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
try {
 $key=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer).Trim()
 if($key -notmatch '^re_[A-Za-z0-9_-]{10,}$'){throw 'La clave no tiene el formato esperado de Resend.'}
 [IO.File]::WriteAllLines((Join-Path $projectRoot '.local/resend.env'),@('MAIL_PROVIDER=resend',"RESEND_API_KEY=$key",'RESEND_FROM=onboarding@resend.dev','ALERT_EMAIL_ENABLED=false',"ALERT_EMAIL_TO=$Recipient"),[Text.Encoding]::ASCII)
 Write-Host 'Configuración HTTPS guardada en .local/resend.env. Todavía no se cambió el correo que está funcionando.'
}finally{[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer);$key=$null;$secret.Dispose()}
