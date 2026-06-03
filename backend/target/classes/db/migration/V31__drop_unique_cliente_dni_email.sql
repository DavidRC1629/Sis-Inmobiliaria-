-- Permite que el mismo cliente pueda figurar en varios lotes.
-- Se eliminan restricciones únicas sobre DNI y correo en clientes.

SET @dni_index := (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'clientes'
      AND column_name = 'dni'
      AND non_unique = 0
      AND index_name <> 'PRIMARY'
    LIMIT 1
);

SET @sql_dni := IF(
    @dni_index IS NOT NULL,
    CONCAT('ALTER TABLE clientes DROP INDEX ', @dni_index),
    'SELECT 1'
);
PREPARE stmt_dni FROM @sql_dni;
EXECUTE stmt_dni;
DEALLOCATE PREPARE stmt_dni;

SET @email_index := (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'clientes'
      AND column_name = 'email'
      AND non_unique = 0
      AND index_name <> 'PRIMARY'
    LIMIT 1
);

SET @sql_email := IF(
    @email_index IS NOT NULL,
    CONCAT('ALTER TABLE clientes DROP INDEX ', @email_index),
    'SELECT 1'
);
PREPARE stmt_email FROM @sql_email;
EXECUTE stmt_email;
DEALLOCATE PREPARE stmt_email;
