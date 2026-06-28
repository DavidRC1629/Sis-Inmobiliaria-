-- V10__Add_lotes_disponibles_to_parcelas.sql
-- Agregar columna lotes_disponibles a la tabla parcelas

ALTER TABLE parcelas ADD COLUMN lotes_disponibles INT NOT NULL DEFAULT 0;

-- Actualizar lotes_disponibles para que coincida con la cantidad de lotes existentes
UPDATE parcelas p 
SET lotes_disponibles = (
    SELECT COUNT(*) 
    FROM lotes l 
    WHERE l.parcela_id = p.id
);
