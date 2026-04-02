-- Crear tabla manzanas vinculada a projects
CREATE TABLE IF NOT EXISTS manzanas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    letra VARCHAR(2) NOT NULL,
    project_id BIGINT NOT NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    UNIQUE KEY unique_letra_project (letra, project_id),
    INDEX idx_project_id (project_id)
);

-- Crear tabla lotes vinculada a manzanas
CREATE TABLE IF NOT EXISTS lotes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero INT NOT NULL,
    area DECIMAL(10,2),
    precio DECIMAL(15,2),
    estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    manzana_id BIGINT NOT NULL,
    FOREIGN KEY (manzana_id) REFERENCES manzanas(id) ON DELETE CASCADE,
    INDEX idx_manzana_id (manzana_id),
    INDEX idx_estado (estado)
);
