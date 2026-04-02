CREATE TABLE IF NOT EXISTS cronograma_pago_separaciones (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    contrato_id BIGINT NOT NULL,
    fecha_pago DATE NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    tipo_pago VARCHAR(40) NULL,
    estado_pago VARCHAR(20) NOT NULL,
    notas VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pago_separacion_contrato FOREIGN KEY (contrato_id) REFERENCES cronograma_contratos(id)
);

CREATE INDEX idx_pago_separacion_contrato ON cronograma_pago_separaciones(contrato_id);
CREATE INDEX idx_pago_separacion_fecha ON cronograma_pago_separaciones(fecha_pago);
