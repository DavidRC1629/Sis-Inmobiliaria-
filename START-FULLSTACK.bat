@echo off
echo ================================================
echo   SISAROVI - Sistema Inmobiliario Full Stack
echo ================================================
echo.
echo ** IMPORTANTE: Ejecuta primero en MySQL Workbench **
echo.
echo DROP DATABASE IF EXISTS sisarovi_db;
echo CREATE DATABASE sisarovi_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
echo.
pause

echo.
echo [1/2] Iniciando Backend (Spring Boot)...
start "Backend - Puerto 8080" cmd /k "cd /d C:\Users\david\Videos\SisArovi\backend && C:\Users\david\.maven\maven-3.9.12\bin\mvn.cmd spring-boot:run"

timeout /t 10 /nobreak > nul

echo [2/2] Iniciando Frontend (Angular)...
start "Frontend - Puerto 4200" cmd /k "cd /d C:\Users\david\Videos\SisArovi\frontsisarovi && ng serve --open"

echo.
echo ================================================
echo   Sistema iniciado correctamente
echo ================================================
echo   Backend:  http://localhost:8080
echo   Frontend: http://localhost:4200
echo   Usuario: admin / Password: admin123
echo ================================================
pause
