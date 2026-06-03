-- Permite repetir número de lote en distintas manzanas de una misma parcela.
-- Regla correcta: único por (parcela_id, manzana_id, numero).

SET @has_old_uk := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'lotes'
    AND index_name = 'uk_lote_numero_parcela'
);

SET @drop_old_uk_sql := IF(
  @has_old_uk > 0,
  'ALTER TABLE lotes DROP INDEX uk_lote_numero_parcela',
  'SELECT 1'
);
PREPARE stmt_drop_old_uk FROM @drop_old_uk_sql;
EXECUTE stmt_drop_old_uk;
DEALLOCATE PREPARE stmt_drop_old_uk;

SET @has_old_uq := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'lotes'
    AND index_name = 'uq_lote_numero_parcela'
);

SET @drop_old_uq_sql := IF(
  @has_old_uq > 0,
  'ALTER TABLE lotes DROP INDEX uq_lote_numero_parcela',
  'SELECT 1'
);
PREPARE stmt_drop_old_uq FROM @drop_old_uq_sql;
EXECUTE stmt_drop_old_uq;
DEALLOCATE PREPARE stmt_drop_old_uq;

SET @has_new_uk := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'lotes'
    AND index_name = 'uk_lote_parcela_manzana_numero'
);

SET @create_new_uk_sql := IF(
  @has_new_uk = 0,
  'ALTER TABLE lotes ADD CONSTRAINT uk_lote_parcela_manzana_numero UNIQUE (parcela_id, manzana_id, numero)',
  'SELECT 1'
);
PREPARE stmt_create_new_uk FROM @create_new_uk_sql;
EXECUTE stmt_create_new_uk;
DEALLOCATE PREPARE stmt_create_new_uk;
