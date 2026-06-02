@echo off
echo.
echo ===============================================
echo    REINICIANDO SERVICIOS - SISAROVI
echo ===============================================
echo.

echo [1/4] Deteniendo servicios existentes...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080') do taskkill /F /PID %%a 2>nul
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :4200') do taskkill /F /PID %%a 2>nul
timeout /t 3 >nul

echo.
echo [2/4] Iniciando BACKEND (Puerto 8080)...
start "Backend - Puerto 8080" cmd /k "cd /d %~dp0backend && echo. && echo === BACKEND SISAROVI === && echo. && mvn spring-boot:run"
timeout /t 8 >nul

echo.
echo [3/4] Iniciando FRONTEND (Puerto 4200)...
start "Frontend - Puerto 4200" cmd /k "cd /d %~dp0frontsisarovi && echo. && echo === FRONTEND SISAROVI === && echo. && npm start"
timeout /t 5 >nul

echo.
echo ===============================================
echo    SERVICIOS INICIADOS
echo ===============================================
echo.
echo   Backend:  http://localhost:8080
echo   Frontend: http://localhost:4200
echo.
echo Por favor:
echo   1. Espera 30 segundos a que los servicios inicien
echo   2. Cierra sesion en el navegador
echo   3. Vuelve a iniciar sesion con: admin / admin123
echo   4. Intenta crear el proyecto
echo.
echo ===============================================
echo.
pause
