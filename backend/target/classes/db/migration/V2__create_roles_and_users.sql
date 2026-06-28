-- V2__create_roles_and_users.sql
-- Crea las tablas roles y users, e inserta los datos iniciales para Flyway (MySQL)

CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB;

INSERT IGNORE INTO roles (id, name) VALUES 
    (1, 'ROLE_ADMIN'),
    (2, 'ROLE_USER');

-- Contraseña admin123 (hash BCrypt)
INSERT IGNORE INTO users (username, password, email, role_id) VALUES 
    ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@sisarovi.com', 1);
