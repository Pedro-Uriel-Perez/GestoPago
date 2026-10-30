-- Esquema consolidado SOLO para pruebas de integracion sobre H2.
-- Refleja el estado final de src/main/resources/db/migration (V1..V12),
-- pero sin la sintaxis exclusiva de PostgreSQL que H2 no soporta
-- (ON CONFLICT, UPDATE ... FROM, ALTER TABLE con varias acciones, el
-- operador de regex ~). Si se agrega una migracion real que cambie
-- alguna de estas tablas, hay que reflejarla aqui tambien.

CREATE TABLE gestopago_tokens (
    id                  SERIAL PRIMARY KEY,
    id_distribuidor     INTEGER      NOT NULL,
    codigo_dispositivo  VARCHAR(100) NOT NULL,
    token               TEXT         NOT NULL,
    token_type          VARCHAR(50),
    expires_in          BIGINT,
    fecha_creacion      TIMESTAMP    NOT NULL DEFAULT NOW(),
    fecha_actualizacion TIMESTAMP    NOT NULL DEFAULT NOW(),
    activo              BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_gestopago_tokens UNIQUE (id_distribuidor, codigo_dispositivo)
);

CREATE TABLE gestopago_productos (
    id                     SERIAL PRIMARY KEY,
    id_producto            INTEGER   NOT NULL,
    id_servicio            INTEGER,
    servicio               TEXT,
    producto               TEXT,
    id_cat_tipo_servicio   INTEGER,
    tipo_front             INTEGER,
    has_digito_verificador BOOLEAN,
    legend                 TEXT,
    tipo_referencia        TEXT,
    precio                 TEXT,
    fecha_actualizacion    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_gestopago_productos_id_producto UNIQUE (id_producto)
);

CREATE TABLE nacionalidades (
    id     SERIAL PRIMARY KEY,
    nombre TEXT NOT NULL UNIQUE
);

INSERT INTO nacionalidades (nombre) VALUES ('MEXICANA'), ('ESTADOUNIDENSE'), ('OTRA');

CREATE TABLE clientes (
    id                   SERIAL PRIMARY KEY,
    nombre               TEXT          NOT NULL,
    segundo_nombre       TEXT,
    apellido_paterno     TEXT          NOT NULL,
    apellido_materno     TEXT          NOT NULL,
    fecha_nacimiento     DATE          NOT NULL,
    curp                 TEXT          NOT NULL,
    rfc                  TEXT          NOT NULL,
    sexo                 TEXT          NOT NULL,
    nacionalidad_id      INTEGER       NOT NULL,
    estado_civil         TEXT          NOT NULL,
    correo_electronico   TEXT          NOT NULL,
    telefono_movil       TEXT          NOT NULL,
    telefono_alternativo TEXT,
    ocupacion            TEXT          NOT NULL,
    empresa              TEXT          NOT NULL,
    ingreso_mensual      NUMERIC(12,2) NOT NULL,
    activo               BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_registro       TIMESTAMP     NOT NULL DEFAULT NOW(),
    fecha_actualizacion  TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_clientes_curp UNIQUE (curp),
    CONSTRAINT uq_clientes_rfc UNIQUE (rfc),
    CONSTRAINT uq_clientes_correo UNIQUE (correo_electronico),
    CONSTRAINT fk_clientes_nacionalidad FOREIGN KEY (nacionalidad_id) REFERENCES nacionalidades (id)
);

CREATE TABLE domicilios (
    id              SERIAL PRIMARY KEY,
    cliente_id      INTEGER NOT NULL,
    calle           TEXT    NOT NULL,
    numero_exterior TEXT    NOT NULL,
    numero_interior TEXT,
    colonia         TEXT    NOT NULL,
    municipio       TEXT    NOT NULL,
    estado          TEXT    NOT NULL,
    codigo_postal   TEXT    NOT NULL,
    pais            TEXT    NOT NULL,
    CONSTRAINT uq_domicilios_cliente UNIQUE (cliente_id),
    CONSTRAINT fk_domicilios_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id)
);

CREATE TABLE cuentas (
    id             SERIAL PRIMARY KEY,
    cliente_id     INTEGER   NOT NULL,
    numero_cuenta  TEXT      NOT NULL,
    estatus        TEXT      NOT NULL DEFAULT 'ACTIVA',
    fecha_apertura TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cuentas_cliente UNIQUE (cliente_id),
    CONSTRAINT uq_cuentas_numero_cuenta UNIQUE (numero_cuenta),
    CONSTRAINT fk_cuentas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id)
);

CREATE TABLE saldos (
    id               SERIAL PRIMARY KEY,
    cuenta_id        INTEGER       NOT NULL,
    monto            NUMERIC(15,2) NOT NULL,
    tipo_movimiento  TEXT          NOT NULL,
    fecha_movimiento TIMESTAMP     NOT NULL DEFAULT NOW(),
    descripcion      TEXT,
    CONSTRAINT fk_saldos_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas (id),
    CONSTRAINT ck_saldos_monto_no_negativo CHECK (monto >= 0),
    CONSTRAINT ck_saldos_tipo_movimiento CHECK (tipo_movimiento IN ('APERTURA', 'DEPOSITO', 'RETIRO', 'AJUSTE', 'COMPRA'))
);

CREATE TABLE logins (
    id                  SERIAL PRIMARY KEY,
    cliente_id          INTEGER   NOT NULL,
    correo_login        TEXT      NOT NULL,
    nombre_cifrado      TEXT      NOT NULL,
    jwt_token           TEXT,
    datos_biometricos   TEXT,
    sesion_activa       BOOLEAN   NOT NULL DEFAULT FALSE,
    fecha_ultimo_acceso TIMESTAMP,
    fecha_creacion      TIMESTAMP NOT NULL DEFAULT NOW(),
    password_hash       TEXT,
    CONSTRAINT uq_logins_cliente UNIQUE (cliente_id),
    CONSTRAINT fk_logins_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id)
);

CREATE TABLE conciliaciones_pendientes (
    id             SERIAL PRIMARY KEY,
    cuenta_id      INTEGER       NOT NULL,
    cliente_id     INTEGER       NOT NULL,
    upc            TEXT          NOT NULL,
    id_producto    INTEGER       NOT NULL,
    monto          NUMERIC(15,2) NOT NULL,
    fecha_creacion TIMESTAMP     NOT NULL DEFAULT NOW(),
    resuelta       BOOLEAN       NOT NULL DEFAULT FALSE,
    observaciones  TEXT,
    CONSTRAINT fk_conciliaciones_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas (id)
);
