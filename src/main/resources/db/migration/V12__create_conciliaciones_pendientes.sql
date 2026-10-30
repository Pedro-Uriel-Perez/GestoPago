CREATE TABLE IF NOT EXISTS conciliaciones_pendientes (
    id               SERIAL PRIMARY KEY,
    cuenta_id        INTEGER       NOT NULL,
    cliente_id       INTEGER       NOT NULL,
    upc              TEXT          NOT NULL,
    id_producto      INTEGER       NOT NULL,
    monto            NUMERIC(15,2) NOT NULL,
    fecha_creacion   TIMESTAMP     NOT NULL DEFAULT NOW(),
    resuelta         BOOLEAN       NOT NULL DEFAULT FALSE,
    observaciones    TEXT,

    CONSTRAINT fk_conciliaciones_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas (id)
);

CREATE INDEX IF NOT EXISTS ix_conciliaciones_pendientes_resuelta ON conciliaciones_pendientes (resuelta);
