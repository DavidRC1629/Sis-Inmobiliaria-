-- Limpieza de columnas legacy en lotes
-- 1) Copiar datos de aream2 -> area_m2 si corresponde
-- 2) Eliminar columna manzana (ya se usa manzana_id)
-- 3) Eliminar columna aream2 legacy

SET @has_area_m2 := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'lotes'
    AND column_name = 'area_m2'
);

SET @has_aream2 := (
  SELECT COUNT(1)
  FROM information_schema.columnsxxxnb
  WHERE table_schema = DATABASE()
    AND table_name = 'lotes'
    AND column_name = 'aream2'
);

SET @copy_sql := IF(
  @has_area_m2 > 0 AND @has_aream2 > 0,
  'UPDATE lotes SET area_m2 = aream2 WHERE area_m2 IS NULL AND aream2 IS NOT NULL',
  'SELECT 1'
);
PREPARE stmt_copy FROM @copy_sql;
EXECUTE stmt_copy;
DEALLOCATE PREPARE stmt_copy;

SET @has_idx_manzana := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'lotes'
    AND index_name = 'idx_lote_manzana'
);

SET @drop_idx_sql := IF(
  @has_idx_manzana > 0,
  'ALTER TABLE lotes DROP INDEX idx_lote_manzana',
  'SELECT 1'
);
PREPARE stmt_drop_idx FROM @drop_idx_sql;
EXECUTE stmt_drop_idx;
DEALLOCATE PREPARE stmt_drop_idx;

SET @has_manzana_col := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'lotes'
    AND column_name = 'manzana'
);

SET @drop_manzana_sql := IF(
  @has_manzana_col > 0,
  'ALTER TABLE lotes DROP COLUMN manzana',
  'SELECT 1'
);
PREPARE stmt_drop_manzana FROM @drop_manzana_sql;
EXECUTE stmt_drop_manzana;
DEALLOCATE PREPARE stmt_drop_manzana;

SET @has_aream2_col := (
  SELECT COUNT(1)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'lotes'
    AND column_name = 'aream2'
);

SET @drop_aream2_sql := IF(
  @has_aream2_col > 0,
  'ALTER TABLE lotes DROP COLUMN aream2',
  'SELECT 1'
);
PREPARE stmt_drop_aream2 FROM @drop_aream2_sql;
EXECUTE stmt_drop_aream2;
DEALLOCATE PREPARE stmt_drop_aream2;
