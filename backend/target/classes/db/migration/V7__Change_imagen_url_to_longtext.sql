-- Cambiar el tipo de columna imagen_url de VARCHAR(500) a LONGTEXT
-- para soportar imágenes en base64 que pueden ser muy grandes
ALTER TABLE projects MODIFY COLUMN imagen_url LONGTEXT;
