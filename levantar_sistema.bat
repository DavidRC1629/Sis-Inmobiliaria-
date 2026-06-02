@echo off
REM Script para iniciar Backend (Spring Boot) y Frontend (Angular)
REM Ubicar este archivo en la raíz del proyecto: c:\Users\david\Videos\SisArovi - copia\

SET BACKEND_DIR=c:\Users\david\Videos\SisArovi - copia\backend
SET FRONT_DIR=c:\Users\david\Videos\SisArovi - copia\frontsisarovi

echo Iniciando backend en nueva ventana...
start "Backend" cmd /k "cd /d "%BACKEND_DIR%" && if exist mvnw ( .\mvnw spring-boot:run ) else ( mvn spring-boot:run )"

REM Esperar a que el backend arranque (ajusta si necesitas más tiempo)
timeout /t 12 /nobreak >nul

echo Mostrando configuración de frontend (environments/environment.ts):
type "%FRONT_DIR%\src\environments\environment.ts"

echo Iniciando frontend en nueva ventana...
start "Frontend" cmd /k "cd /d "%FRONT_DIR%" && npm install && if exist node_modules\@angular\cli ( ng serve --open ) else ( npm install -g @angular/cli && ng serve --open )"

echo Script ejecutado. Revisa las ventanas "Backend" y "Frontend" para logs.
pause
