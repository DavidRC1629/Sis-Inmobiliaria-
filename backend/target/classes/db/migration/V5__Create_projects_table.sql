CREATE TABLE IF NOT EXISTS projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    propietario VARCHAR(200) NOT NULL,
    num_lotes INT NOT NULL,
    num_manzanas INT NOT NULL CHECK (num_manzanas >= 1 AND num_manzanas <= 27),
    imagen_url VARCHAR(500),
    created_by BIGINT NOT NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_created_by (created_by),
    INDEX idx_nombre (nombre)
);
