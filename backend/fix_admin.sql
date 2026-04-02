-- Verificar estado actual del usuario admin
SELECT 'Estado actual del usuario admin:' as mensaje;
SELECT dni, nombres, primer_apellido, estado, enabled FROM users WHERE dni='00000000';

-- Actualizar usuario admin a ACTIVO
UPDATE users SET estado = 'ACTIVO', enabled = 1 WHERE dni = '00000000';

-- Verificar que se aplicó correctamente
SELECT 'Estado después de la actualización:' as mensaje;
SELECT dni, nombres, primer_apellido, estado, enabled FROM users WHERE dni='00000000';
