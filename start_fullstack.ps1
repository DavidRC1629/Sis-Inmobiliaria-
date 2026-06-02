# start_fullstack.ps1
# Ejecuta el backend en una ventana nueva y pregunta antes de iniciar el frontend

param(
    [int]$BackendDelaySeconds = 12
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Definition
$backendDir = Join-Path $root 'backend'
$frontDir = Join-Path $root 'frontsisarovi'

Write-Host "Iniciando backend en nueva ventana..." -ForegroundColor Cyan

$mvnwPath = Join-Path $backendDir 'mvnw'
if (Test-Path $mvnwPath) {
    $backendCmd = ".\mvnw spring-boot:run"
} else {
    $backendCmd = "mvn spring-boot:run"
}

Start-Process powershell -ArgumentList "-NoExit -Command Set-Location -Path '$backendDir'; $backendCmd"

Write-Host "Esperando $BackendDelaySeconds segundos para que el backend arranque..." -ForegroundColor Yellow
Start-Sleep -Seconds $BackendDelaySeconds

$answer = Read-Host "¿Iniciar frontend ahora? (S/N)"
if ($answer -match '^[sS]') {
    Write-Host "Iniciando frontend en nueva ventana..." -ForegroundColor Cyan
    $frontendCmd = "npm install; npx ng serve --open"
    Start-Process powershell -ArgumentList "-NoExit -Command Set-Location -Path '$frontDir'; $frontendCmd"
} else {
    Write-Host "Frontend no iniciado. Puedes iniciarlo posteriormente ejecutando 'npm install' y 'ng serve --open' en $frontDir" -ForegroundColor Green
}

Write-Host "Script finalizado." -ForegroundColor Green
