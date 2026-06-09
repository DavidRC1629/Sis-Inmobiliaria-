CREATE TABLE IF NOT EXISTS devoluciones (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lote_id BIGINT NOT NULL,
    lote_numero INT NOT NULL,
    manzana VARCHAR(120) NOT NULL,
    parcela_nombre VARCHAR(120) NOT NULL,
    etapa_numero INT NOT NULL,
    proyecto_nombre VARCHAR(200) NOT NULL,
    monto_total DECIMAL(19, 2) NOT NULL,
    monto_pagado DECIMAL(19, 2) NOT NULL DEFAULT 0,
    dias INT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin_estimada DATE NOT NULL,
    descripcion TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'EN_CURSO',
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME NOT NULL,
    INDEX idx_devoluciones_estado (estado),
    INDEX idx_devoluciones_fecha_creacion (fecha_creacion),
    INDEX idx_devoluciones_lote (lote_id)
);

CREATE TABLE IF NOT EXISTS devolucion_pagos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    devolucion_id BIGINT NOT NULL,
    monto DECIMAL(19, 2) NOT NULL,
    fecha_pago DATE NOT NULL,
    descripcion TEXT NOT NULL,
    medio_pago VARCHAR(40) NOT NULL,
    fecha_registro DATETIME NOT NULL,
    CONSTRAINT fk_devolucion_pagos_devolucion
        FOREIGN KEY (devolucion_id) REFERENCES devoluciones(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_devolucion_pagos_devolucion ON devolucion_pagos(devolucion_id);