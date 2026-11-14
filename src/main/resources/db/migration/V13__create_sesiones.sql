-- Una sesion por dispositivo (WEB / MOVIL) para que el mismo cliente pueda
-- estar conectado en la plataforma web y en la app movil al mismo tiempo.
-- Antes la sesion vivia en logins (un solo jwt_token por cliente), asi que
-- iniciar sesion en un dispositivo cerraba la del otro.
-- jwt_token se guarda cifrado (AES) igual que en logins; identificador es el
-- "sid" que viaja dentro del JWT y permite ubicar la sesion sin descifrar.
CREATE TABLE IF NOT EXISTS sesiones (
    id                  SERIAL PRIMARY KEY,
    cliente_id          INTEGER     NOT NULL,
    identificador       VARCHAR(36) NOT NULL,
    dispositivo         VARCHAR(10) NOT NULL,
    jwt_token           TEXT,
    activa              BOOLEAN     NOT NULL DEFAULT TRUE,
    fecha_inicio        TIMESTAMP   NOT NULL DEFAULT NOW(),
    fecha_ultimo_acceso TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_sesiones_identificador UNIQUE (identificador),
    CONSTRAINT fk_sesiones_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT ck_sesiones_dispositivo CHECK (dispositivo IN ('WEB', 'MOVIL'))
);

CREATE INDEX IF NOT EXISTS ix_sesiones_cliente_activa ON sesiones (cliente_id, activa);
