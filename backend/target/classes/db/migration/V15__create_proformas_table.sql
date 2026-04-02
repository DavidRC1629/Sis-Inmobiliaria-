CREATE TABLE IF NOT EXISTS proformas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    proyecto VARCHAR(200),
    cliente_nombre VARCHAR(200),
    cliente_dni VARCHAR(20),
    cliente_celular VARCHAR(30),
    asesor VARCHAR(150),
    fecha_emision DATE,
    fecha_vencimiento DATE,
    precio_contado DECIMAL(12,2),
    detalle_json LONGTEXT,
    created_by VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_proformas_codigo ON proformas(codigo);
CREATE INDEX idx_proformas_cliente_nombre ON proformas(cliente_nombre);
CREATE INDEX idx_proformas_created_at ON proformas(created_at);