-- Add email column to users and enforce uniqueness for non-null values.
-- Uses defensive checks so it can run safely even if the column/index already exist.

SET @col_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND COLUMN_NAME = 'email'
);

SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE users ADD COLUMN email VARCHAR(120) NULL AFTER dni',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'users'
    AND INDEX_NAME = 'uk_users_email'
);

SET @sql := IF(
  @idx_exists = 0,
  'CREATE UNIQUE INDEX uk_users_email ON users (email)',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
