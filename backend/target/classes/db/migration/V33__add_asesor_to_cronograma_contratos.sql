-- Adds advisor information for lot acquisition contracts.
ALTER TABLE cronograma_contratos
ADD COLUMN asesor VARCHAR(120) NULL;
