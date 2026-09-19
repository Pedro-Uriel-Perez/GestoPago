CREATE TABLE IF NOT EXISTS gestopago_productos (
    id                    SERIAL PRIMARY KEY,
    id_producto           INTEGER   NOT NULL,
    id_servicio           INTEGER,
    servicio              TEXT,
    producto              TEXT,
    id_cat_tipo_servicio  INTEGER,
    tipo_front            INTEGER,
    has_digito_verificador BOOLEAN,
    legend                TEXT,
    fecha_actualizacion   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_gestopago_productos_id_producto UNIQUE (id_producto)
);
