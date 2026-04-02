-- V8__Restructure_to_etapas_parcelas_lotes.sql
-- Migración para la nueva arquitectura: Project → Etapas → Parcelas → Lotes

-- Eliminar tablas antiguas
DROP TABLE IF EXISTS lotes;
DROP TABLE IF EXISTS manzanas;
DROP TABLE IF EXISTS proyectos;

-- Crear tabla projects
CREATE TABLE IF NOT EXISTS projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    imagen_url VARCHAR(500),
    created_by_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_created_by FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- Crear tabla etapas
CREATE TABLE IF NOT EXISTS etapas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_etapa INT NOT NULL,
    project_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_etapa_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT uk_etapa_numero_project UNIQUE (project_id, numero_etapa)
);

-- Crear tabla parcelas
CREATE TABLE IF NOT EXISTS parcelas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    num_manzanas INT NOT NULL,
    num_lotes INT NOT NULL,
    propietario VARCHAR(255) NOT NULL,
    etapa_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_parcela_etapa FOREIGN KEY (etapa_id) REFERENCES etapas(id) ON DELETE CASCADE,
    CONSTRAINT chk_num_manzanas CHECK (num_manzanas >= 1 AND num_manzanas <= 27),
    CONSTRAINT chk_num_lotes CHECK (num_lotes >= 1)
);

-- Crear tabla lotes
CREATE TABLE IF NOT EXISTS lotes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero INT NOT NULL,
    calle VARCHAR(255),
    perimetro DECIMAL(10, 2),
    area_m2 DECIMAL(10, 2),
    medida_frente DECIMAL(10, 2),
    medida_izquierda DECIMAL(10, 2),
    medida_derecha DECIMAL(10, 2),
    medida_fondo DECIMAL(10, 2),
    numero_partida VARCHAR(100),
    propietario VARCHAR(255) NOT NULL,
    nuevo_propietario VARCHAR(255),
    manzana VARCHAR(10) NOT NULL,
    parcela_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lote_parcela FOREIGN KEY (parcela_id) REFERENCES parcelas(id) ON DELETE CASCADE,
    CONSTRAINT uk_lote_numero_parcela UNIQUE (parcela_id, numero)
);

-- Crear índices para mejorar rendimiento
CREATE INDEX idx_etapa_project ON etapas(project_id);
CREATE INDEX idx_parcela_etapa ON parcelas(etapa_id);
CREATE INDEX idx_parcela_propietario ON parcelas(propietario);
CREATE INDEX idx_lote_parcela ON lotes(parcela_id);
CREATE INDEX idx_lote_manzana ON lotes(manzana);
CREATE INDEX idx_lote_propietario ON lotes(propietario);
CREATE INDEX idx_lote_nuevo_propietario ON lotes(nuevo_propietario);
