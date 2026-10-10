CREATE TABLE IF NOT EXISTS domicilios (
    id                SERIAL PRIMARY KEY,
    cliente_id        INTEGER NOT NULL,
    calle             TEXT    NOT NULL,
    numero_exterior   TEXT    NOT NULL,
    numero_interior   TEXT,
    colonia           TEXT    NOT NULL,
    municipio         TEXT    NOT NULL,
    estado            TEXT    NOT NULL,
    codigo_postal     TEXT    NOT NULL,
    pais              TEXT    NOT NULL,

    CONSTRAINT uq_domicilios_cliente UNIQUE (cliente_id),
    CONSTRAINT fk_domicilios_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT ck_domicilios_codigo_postal_formato CHECK (codigo_postal ~ '^[0-9]{5}$')
);
