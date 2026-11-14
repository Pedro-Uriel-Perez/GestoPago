-- "Log evidence" de la especificacion de PuntoRed/GestoPago: es obligatorio
-- guardar las peticiones y respuestas intercambiadas con el proveedor para
-- revisiones o aclaraciones. Se guarda el payload en claro (antes de
-- cifrarlo, sin token ni llaves) y la respuesta recibida, o el error si no
-- hubo respuesta.
CREATE TABLE IF NOT EXISTS gestopago_bitacora (
    id          SERIAL PRIMARY KEY,
    operacion   VARCHAR(30) NOT NULL,
    upc         VARCHAR(40),
    peticion    TEXT,
    respuesta   TEXT,
    codigo      VARCHAR(10),
    error       TEXT,
    duracion_ms BIGINT,
    fecha       TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ix_gestopago_bitacora_upc ON gestopago_bitacora (upc);
CREATE INDEX IF NOT EXISTS ix_gestopago_bitacora_fecha ON gestopago_bitacora (fecha);
