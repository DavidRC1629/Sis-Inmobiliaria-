# Script para iniciar el sistema SisArovi de forma limpia
Write-Host "`n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "   SISAROVI - Iniciando Sistema" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`n" -ForegroundColor Cyan

# 1. Matar TODOS los procesos existentes
Write-Host "🔴 Deteniendo servicios existentes..." -ForegroundColor Yellow
Get-Process -Name java, node -ErrorAction SilentlyContinue | Stop-Process -Force
Start-Sleep -Seconds 3

# 2. Verificar que están muertos
$remaining = Get-Process -Name java, node -ErrorAction SilentlyContinue
if ($remaining) {
    Write-Host "⚠️  Todavía hay procesos, matando con taskkill..." -ForegroundColor Red
    taskkill /F /IM java.exe /T 2>$null
    taskkill /F /IM node.exe /T 2>$null
    Start-Sleep -Seconds 2
}

Write-Host "✅ Todos los servicios detenidos`n" -ForegroundColor Green

# 3. Iniciar Backend
Write-Host "🚀 Iniciando Backend..." -ForegroundColor Yellow
Start-Process cmd -ArgumentList "/k", "cd /d c:\Users\david\Videos\SisArovi\backend `& java -jar target\inmobiliario-backend-1.0.0.jar"
Start-Sleep -Seconds 10

# 4. Iniciar Frontend  
Write-Host "🌐 Iniciando Frontend..." -ForegroundColor Cyan
Start-Process cmd -ArgumentList "/k", "cd /d c:\Users\david\Videos\SisArovi\frontsisarovi `& npm start"
Start-Sleep -Seconds 8

# 5. Verificar
Write-Host "`n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Green
Write-Host "     ✅ SISTEMA INICIADO" -ForegroundColor Green
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`n" -ForegroundColor Green

$backend = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
$frontend = Get-NetTCPConnection -LocalPort 4200 -State Listen -ErrorAction SilentlyContinue
$javaCount = @(Get-Process -Name java -ErrorAction SilentlyContinue).Count

if ($backend) {
    Write-Host "✅ Backend:  http://localhost:8080" -ForegroundColor Cyan
    Write-Host "   Procesos Java activos: $javaCount" -ForegroundColor Gray
} else {
    Write-Host "⏳ Backend iniciando... (espera 10s más)" -ForegroundColor Yellow
}

if ($frontend) {
    Write-Host "✅ Frontend: http://localhost:4200`n" -ForegroundColor Cyan
} else {
    Write-Host "⏳ Frontend iniciando... (espera 10s más)`n" -ForegroundColor Yellow
}

Write-Host "🔐 Credenciales:" -ForegroundColor Yellow
Write-Host "   DNI: 00000000" -ForegroundColor White
Write-Host "   Password: admin123`n" -ForegroundColor White

Write-Host "🔧 Configuración actual:" -ForegroundColor Green
Write-Host "   • Sin @PreAuthorize en controladores" -ForegroundColor White
Write-Host "   • Seguridad en SecurityConfig" -ForegroundColor White
Write-Host "   • /api/projects/** requiere autenticación`n" -ForegroundColor White

Write-Host "📋 CREA EL PROYECTO - DEBE FUNCIONAR AHORA`n" -ForegroundColor Cyan
