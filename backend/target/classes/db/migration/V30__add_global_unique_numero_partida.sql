-- Fuerza unicidad global del número de partida para todos los lotes.
-- 1) Normaliza valores vacíos a NULL.
-- 2) Para duplicados históricos, conserva el registro más antiguo y pone NULL en los demás
--    para no inventar números de partida.
-- 3) Crea restricción única global.

UPDATE lotes
SET numero_partida = NULL
WHERE numero_partida IS NOT NULL
    AND TRIM(numero_partida) = '';

UPDATE lotes l
JOIN (
        SELECT LOWER(TRIM(numero_partida)) AS partida_key, MIN(id) AS keep_id
        FROM lotes
        WHERE numero_partida IS NOT NULL
            AND TRIM(numero_partida) <> ''
        GROUP BY LOWER(TRIM(numero_partida))
        HAVING COUNT(*) > 1
) d ON LOWER(TRIM(l.numero_partida)) = d.partida_key
SET l.numero_partida = NULL
WHERE l.id <> d.keep_id;

SET @constraint_exists := (
        SELECT COUNT(*)
        FROM information_schema.table_constraints
        WHERE table_schema = DATABASE()
            AND table_name = 'lotes'
            AND constraint_name = 'uk_lotes_numero_partida_global'
            AND constraint_type = 'UNIQUE'
);

SET @sql := IF(
        @constraint_exists = 0,
        'ALTER TABLE lotes ADD CONSTRAINT uk_lotes_numero_partida_global UNIQUE (numero_partida)',
        'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
