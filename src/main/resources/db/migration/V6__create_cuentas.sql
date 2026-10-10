CREATE TABLE IF NOT EXISTS cuentas (
    id              SERIAL PRIMARY KEY,
    cliente_id      INTEGER   NOT NULL,
    numero_cuenta   TEXT      NOT NULL,
    estatus         TEXT      NOT NULL DEFAULT 'ACTIVA',
    fecha_apertura  TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_cuentas_cliente UNIQUE (cliente_id),
    CONSTRAINT uq_cuentas_numero_cuenta UNIQUE (numero_cuenta),
    CONSTRAINT fk_cuentas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT ck_cuentas_estatus CHECK (estatus IN ('ACTIVA', 'INACTIVA'))
);

CREATE INDEX IF NOT EXISTS ix_cuentas_estatus ON cuentas (estatus);
