-- Script para activar el usuario admin con DNI 00000000
USE sisarovi_db;

-- Verificar estado actual
SELECT id, dni, nombres, apellido_paterno, estado, enabled, role_id 
FROM users 
WHERE dni = '00000000';

-- Actualizar a ACTIVO
UPDATE users 
SET estado = 'ACTIVO', 
    enabled = 1
WHERE dni = '00000000';

-- Verificar cambio
SELECT id, dni, nombres, apellido_paterno, estado, enabled, role_id 
FROM users 
WHERE dni = '00000000';
