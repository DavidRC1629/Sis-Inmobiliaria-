UPDATE proformas
SET cliente_nombre = LEFT(cliente_nombre, 40)
WHERE cliente_nombre IS NOT NULL
  AND CHAR_LENGTH(cliente_nombre) > 40;

ALTER TABLE proformas
    MODIFY COLUMN cliente_nombre VARCHAR(40);
