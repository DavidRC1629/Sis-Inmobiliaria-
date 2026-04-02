CREATE TABLE IF NOT EXISTS cronograma_pago_cuotas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cuota_id BIGINT NOT NULL,
    fecha_pago DATE NOT NULL,
    monto DECIMAL(12,2) NOT NULL,
    tipo_pago VARCHAR(40) NULL,
    estado_pago VARCHAR(20) NOT NULL,
    notas VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pago_cuota FOREIGN KEY (cuota_id) REFERENCES cronograma_cuotas(id)
);

CREATE INDEX idx_pago_cuota_id ON cronograma_pago_cuotas(cuota_id);
CREATE INDEX idx_pago_fecha ON cronograma_pago_cuotas(fecha_pago);
