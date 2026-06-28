CREATE TABLE IF NOT EXISTS cronograma_contratos (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cliente_id BIGINT NOT NULL,
    tipo_operacion VARCHAR(20) NOT NULL,
    estado VARCHAR(40) NOT NULL,
    fecha_operacion DATE NOT NULL,
    fecha_inicio_cronograma DATE NULL,
    precio_venta DECIMAL(12,2) NOT NULL,
    monto_pagado_total DECIMAL(12,2) NOT NULL DEFAULT 0,
    monto_separacion_objetivo DECIMAL(12,2) NOT NULL DEFAULT 2000,
    monto_separacion_acumulado DECIMAL(12,2) NOT NULL DEFAULT 0,
    saldo_financiar_inicial DECIMAL(12,2) NOT NULL DEFAULT 0,
    plazo_meses INT NOT NULL DEFAULT 12,
    interes_porcentaje DECIMAL(5,2) NOT NULL DEFAULT 0,
    monto_cuota_referencial DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_cronograma_cliente UNIQUE (cliente_id),
    CONSTRAINT fk_cronograma_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

CREATE TABLE IF NOT EXISTS cronograma_cuotas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    contrato_id BIGINT NOT NULL,
    numero_cuota INT NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    monto_cuota DECIMAL(12,2) NOT NULL,
    monto_pagado DECIMAL(12,2) NOT NULL DEFAULT 0,
    estado_pago VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_pago DATE NULL,
    observacion VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cuota_contrato FOREIGN KEY (contrato_id) REFERENCES cronograma_contratos(id),
    CONSTRAINT uq_cuota_contrato_numero UNIQUE (contrato_id, numero_cuota)
);

CREATE INDEX idx_cronograma_estado ON cronograma_contratos(estado);
CREATE INDEX idx_cronograma_tipo_operacion ON cronograma_contratos(tipo_operacion);
CREATE INDEX idx_cuotas_vencimiento ON cronograma_cuotas(fecha_vencimiento);
