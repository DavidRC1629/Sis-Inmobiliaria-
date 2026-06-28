ALTER TABLE clientes
    DROP FOREIGN KEY fk_clientes_lote;

ALTER TABLE clientes
    DROP INDEX uq_clientes_lote;

ALTER TABLE clientes
    ADD INDEX idx_clientes_lote (lote_id);

ALTER TABLE clientes
    ADD CONSTRAINT fk_clientes_lote FOREIGN KEY (lote_id) REFERENCES lotes(id);