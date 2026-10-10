CREATE TABLE IF NOT EXISTS clientes (
    id                    SERIAL PRIMARY KEY,
    nombre                TEXT           NOT NULL,
    segundo_nombre        TEXT,
    apellido_paterno      TEXT           NOT NULL,
    apellido_materno      TEXT           NOT NULL,
    fecha_nacimiento      DATE           NOT NULL,
    curp                  TEXT           NOT NULL,
    rfc                   TEXT           NOT NULL,
    sexo                  TEXT           NOT NULL,
    nacionalidad          TEXT           NOT NULL,
    estado_civil          TEXT           NOT NULL,
    correo_electronico    TEXT           NOT NULL,
    telefono_movil        TEXT           NOT NULL,
    telefono_alternativo  TEXT,
    ocupacion             TEXT           NOT NULL,
    empresa               TEXT           NOT NULL,
    ingreso_mensual       NUMERIC(12,2)  NOT NULL,
    activo                BOOLEAN        NOT NULL DEFAULT TRUE,
    fecha_registro        TIMESTAMP      NOT NULL DEFAULT NOW(),
    fecha_actualizacion   TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_clientes_curp UNIQUE (curp),
    CONSTRAINT uq_clientes_rfc UNIQUE (rfc),
    CONSTRAINT uq_clientes_correo UNIQUE (correo_electronico),

    CONSTRAINT ck_clientes_curp_longitud CHECK (char_length(curp) = 18),
    CONSTRAINT ck_clientes_rfc_longitud CHECK (char_length(rfc) IN (12, 13)),
    CONSTRAINT ck_clientes_correo_longitud CHECK (char_length(correo_electronico) <= 100),
    CONSTRAINT ck_clientes_telefono_movil_formato CHECK (telefono_movil ~ '^[0-9]{10}$'),
    CONSTRAINT ck_clientes_sexo CHECK (sexo IN ('M', 'F')),
    CONSTRAINT ck_clientes_estado_civil CHECK (estado_civil IN ('SOLTERO', 'CASADO', 'DIVORCIADO', 'VIUDO', 'UNION_LIBRE')),
    CONSTRAINT ck_clientes_ingreso_mensual_positivo CHECK (ingreso_mensual > 0),
    CONSTRAINT ck_clientes_fecha_nacimiento_no_futura CHECK (fecha_nacimiento <= CURRENT_DATE)
);

CREATE INDEX IF NOT EXISTS ix_clientes_activo ON clientes (activo);
CREATE INDEX IF NOT EXISTS ix_clientes_fecha_registro ON clientes (fecha_registro);
