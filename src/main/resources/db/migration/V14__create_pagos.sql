-- Historial de pagos del cliente. saldos solo guarda el saldo resultante de
-- cada movimiento; aqui queda el detalle de cada operacion contra GestoPago
-- (servicio, referencia, folio, estado) para poder consultarla despues.
-- estado: EXITOSO (GestoPago confirmo), RECHAZADO (GestoPago la rechazo) o
-- PENDIENTE (no se pudo confirmar el resultado, ver conciliaciones_pendientes).
CREATE TABLE IF NOT EXISTS pagos (
    id                   SERIAL PRIMARY KEY,
    cliente_id           INTEGER       NOT NULL,
    cuenta_id            INTEGER       NOT NULL,
    operacion_id         VARCHAR(36)   NOT NULL,
    id_producto          INTEGER       NOT NULL,
    id_servicio          INTEGER,
    nombre_producto      TEXT,
    nombre_servicio      TEXT,
    telefono             TEXT,
    referencia           TEXT,
    monto                NUMERIC(15,2) NOT NULL,
    estado               VARCHAR(10)   NOT NULL,
    id_transaccion       TEXT,
    numero_autorizacion  TEXT,
    codigo_respuesta     TEXT,
    mensaje              TEXT,
    fecha                TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_pagos_operacion UNIQUE (operacion_id),
    CONSTRAINT fk_pagos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT fk_pagos_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas (id),
    CONSTRAINT ck_pagos_estado CHECK (estado IN ('EXITOSO', 'RECHAZADO', 'PENDIENTE'))
);

CREATE INDEX IF NOT EXISTS ix_pagos_cliente ON pagos (cliente_id, id DESC);
