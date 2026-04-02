-- Reparar estado de Flyway y eliminar columna propietario de projects
USE sisarovi_db;

-- 1. Eliminar registro de migración fallida V9
DELETE FROM flyway_schema_history WHERE version = '9';

-- 2. Verificar y eliminar columna propietario si existe
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA='sisarovi_db'
   AND TABLE_NAME='projects'
   AND COLUMN_NAME='propietario') > 0,
  'ALTER TABLE projects DROP COLUMN propietario;',
  'SELECT "Columna propietario no existe";'
));

PREPARE alterStatement FROM @preparedStatement;
EXECUTE alterStatement;
DEALLOCATE PREPARE alterStatement;

-- 3. Verificar estructura final de projects
SHOW COLUMNS FROM projects;