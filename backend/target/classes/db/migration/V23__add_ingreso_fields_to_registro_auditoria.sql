-- Add new fields to registro_auditoria table for income tracking
ALTER TABLE registro_auditoria ADD COLUMN cliente_nombre VARCHAR(255);
ALTER TABLE registro_auditoria ADD COLUMN cliente_dni VARCHAR(20);
ALTER TABLE registro_auditoria ADD COLUMN monto DECIMAL(19, 2);
ALTER TABLE registro_auditoria ADD COLUMN medios VARCHAR(500);

-- Add index on fecha_hora for date range queries
CREATE INDEX idx_registro_auditoria_fecha_hora ON registro_auditoria(fecha_hora);

-- Add index on accion for filtering by type
CREATE INDEX idx_registro_auditoria_accion ON registro_auditoria(accion);
