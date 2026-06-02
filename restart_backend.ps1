param(
    [int]$Port = 8080
)

Write-Host "Buscando procesos que usan el puerto $Port..." -ForegroundColor Cyan

$pids = @()
try {
    $conns = Get-NetTCPConnection -LocalPort $Port -ErrorAction Stop
    $pids = $conns | Select-Object -ExpandProperty OwningProcess -Unique
} catch {
    Write-Host "Fallback: usando netstat para buscar PID (Get-NetTCPConnection no disponible)." -ForegroundColor Yellow
    $lines = netstat -ano | Select-String ":$Port"
    $pids = $lines | ForEach-Object { if ($_ -match '\s+(\d+)$') { $matches[1] } } | Where-Object { $_ } | Select-Object -Unique
}

if ($pids -and $pids.Count -gt 0) {
    foreach ($processId in $pids) {
        Write-Host "Deteniendo PID $processId..." -ForegroundColor Yellow
        try {
            Stop-Process -Id $processId -Force -ErrorAction Stop
            Write-Host "PID $processId detenido." -ForegroundColor Green
        } catch {
            Write-Host "No se pudo detener PID ${processId}: $($_)" -ForegroundColor Red
        }
    }
} else {
    Write-Host "No se encontraron procesos escuchando en el puerto $Port." -ForegroundColor Green
}

Write-Host "Iniciando backend en el terminal integrado (directorio backend)..." -ForegroundColor Cyan
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location (Join-Path $scriptRoot 'backend')
if (Test-Path '.\mvnw.cmd') {
    Write-Host "Usando wrapper Windows: .\mvnw.cmd spring-boot:run" -ForegroundColor Cyan
    .\mvnw.cmd spring-boot:run
} elseif (Test-Path '.\mvnw') {
    Write-Host "Usando wrapper: .\mvnw spring-boot:run" -ForegroundColor Cyan
    .\mvnw spring-boot:run
} else {
    Write-Host "Usando mvn: mvn spring-boot:run" -ForegroundColor Cyan
    mvn spring-boot:run
}
