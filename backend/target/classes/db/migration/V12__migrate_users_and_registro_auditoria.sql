-- V12__migrate_users_and_registro_auditoria.sql
-- Fusiona migración de users y creación de registro_auditoria

-- Modifica la tabla users para reflejar la entidad User.java actual
ALTER TABLE users
    DROP COLUMN IF EXISTS username,
    DROP COLUMN IF EXISTS email,
    ADD COLUMN IF NOT EXISTS dni VARCHAR(8) NOT NULL UNIQUE,
    ADD COLUMN IF NOT EXISTS nombres VARCHAR(100) NOT NULL,
    ADD COLUMN IF NOT EXISTS primer_apellido VARCHAR(100) NOT NULL,
    ADD COLUMN IF NOT EXISTS segundo_apellido VARCHAR(100),
    ADD COLUMN IF NOT EXISTS estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT FALSE;

-- Si ya existen datos, puedes migrar los valores aquí si es necesario.
-- Asegúrate de que no haya datos en username/email antes de eliminar.

-- Crea la tabla registro_auditoria
CREATE TABLE IF NOT EXISTS registro_auditoria (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    usuario VARCHAR(100) NOT NULL,
    accion VARCHAR(100) NOT NULL,
    descripcion TEXT NOT NULL,
    fecha_hora DATETIME NOT NULL
);
