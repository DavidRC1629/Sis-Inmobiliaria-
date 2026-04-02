-- Tabla para Terrenos Propios (independientes de proyectos)
CREATE TABLE IF NOT EXISTS terrenos_propios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_lote INT NOT NULL,
    calle VARCHAR(255),
    area_m2 DECIMAL(10,2),
    perimetro DECIMAL(10,2),
    medida_frente DECIMAL(10,2),
    medida_fondo DECIMAL(10,2),
    medida_izquierda DECIMAL(10,2),
    medida_derecha DECIMAL(10,2),
    numero_partida VARCHAR(100) NOT NULL,
    precio DECIMAL(15,2) NOT NULL,
    propietario_id BIGINT NOT NULL,
    imagen_url VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_terrenos_numero_partida UNIQUE (numero_partida),
    CONSTRAINT fk_terreno_propietario FOREIGN KEY (propietario_id) REFERENCES clientes(id)
);

-- Índices para búsquedas rápidas
CREATE INDEX idx_terreno_estado ON terrenos_propios(estado);
CREATE INDEX idx_terreno_propietario ON terrenos_propios(propietario_id);
