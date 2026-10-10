CREATE TABLE IF NOT EXISTS saldos (
    id                SERIAL PRIMARY KEY,
    cuenta_id         INTEGER       NOT NULL,
    monto             NUMERIC(15,2) NOT NULL,
    tipo_movimiento   TEXT          NOT NULL,
    fecha_movimiento  TIMESTAMP     NOT NULL DEFAULT NOW(),
    descripcion       TEXT,

    CONSTRAINT fk_saldos_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas (id),
    CONSTRAINT ck_saldos_monto_no_negativo CHECK (monto >= 0),
    CONSTRAINT ck_saldos_tipo_movimiento CHECK (tipo_movimiento IN ('APERTURA', 'DEPOSITO', 'RETIRO', 'AJUSTE'))
);

CREATE INDEX IF NOT EXISTS ix_saldos_cuenta_fecha ON saldos (cuenta_id, fecha_movimiento DESC);
