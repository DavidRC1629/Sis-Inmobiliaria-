ALTER TABLE clientes
    MODIFY COLUMN email VARCHAR(100) NULL;

ALTER TABLE clientes
    ADD COLUMN direccion VARCHAR(255) NOT NULL DEFAULT '';

ALTER TABLE clientes
    ADD COLUMN cliente_desde DATE NULL;

UPDATE clientes
SET cliente_desde = CURDATE()
WHERE cliente_desde IS NULL;

ALTER TABLE clientes
    MODIFY COLUMN cliente_desde DATE NOT NULL;

ALTER TABLE clientes
    ADD COLUMN tipo_relacion VARCHAR(20) NOT NULL DEFAULT 'ADQUISICION';

ALTER TABLE clientes
    ADD COLUMN lote_id BIGINT NULL;

ALTER TABLE clientes
    ADD CONSTRAINT uq_clientes_lote UNIQUE (lote_id);

ALTER TABLE clientes
    ADD CONSTRAINT fk_clientes_lote FOREIGN KEY (lote_id) REFERENCES lotes(id);