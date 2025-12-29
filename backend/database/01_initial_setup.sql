-- =============================================
-- Sistema Inmobiliario - PostgreSQL Database Setup
-- =============================================

-- 1. Crear la base de datos (ejecutar como superusuario postgres)
DROP DATABASE IF EXISTS sisarovi_db;
CREATE DATABASE sisarovi_db
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'Spanish_Spain.1252'
    LC_CTYPE = 'Spanish_Spain.1252'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Conectarse a sisarovi_db antes de ejecutar los siguientes comandos

-- 2. Crear tablas
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- 3. Insertar roles iniciales
INSERT INTO roles (id, name) VALUES 
    (1, 'ROLE_ADMIN'),
    (2, 'ROLE_USER')
ON CONFLICT (name) DO NOTHING;

-- 4. Insertar usuario admin inicial (password: admin123)
-- La contraseña está hasheada con BCrypt
INSERT INTO users (username, password, email, role_id) VALUES 
    ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@sisarovi.com', 1)
ON CONFLICT (username) DO NOTHING;

-- 5. Verificar datos insertados
SELECT * FROM roles;
SELECT id, username, email, role_id, created_at FROM users;

-- =============================================
-- Índices para mejorar rendimiento
-- =============================================
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role_id ON users(role_id);
