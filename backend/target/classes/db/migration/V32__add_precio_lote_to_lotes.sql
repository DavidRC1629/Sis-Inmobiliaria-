SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'lotes'
      AND column_name = 'precio_lote'
);

SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE lotes ADD COLUMN precio_lote DECIMAL(12,2) NULL',
    'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
