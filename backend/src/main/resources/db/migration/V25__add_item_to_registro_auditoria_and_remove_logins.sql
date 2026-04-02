ALTER TABLE registro_auditoria ADD COLUMN item VARCHAR(150);

DELETE FROM registro_auditoria
WHERE accion = 'LOGIN';
