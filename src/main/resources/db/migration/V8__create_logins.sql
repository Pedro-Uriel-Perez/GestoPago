-- correo_login, nombre_cifrado, jwt_token y datos_biometricos se guardan
-- cifrados (AES, ver LoginServiceImpl/AesAttributeConverter) por lo que su
-- tipo en base de datos es TEXT (el texto cifrado en Base64), no el tipo
-- "natural" del dato (el arreglo double[] de datos_biometricos vive solo
-- del lado de Java). sesion_activa y fecha_ultimo_acceso quedan SIN cifrar
-- a proposito: la tarea programada que cierra sesiones inactivas necesita
-- compararlas directamente en SQL.
CREATE TABLE IF NOT EXISTS logins (
    id                    SERIAL PRIMARY KEY,
    cliente_id            INTEGER   NOT NULL,
    correo_login          TEXT      NOT NULL,
    nombre_cifrado        TEXT      NOT NULL,
    jwt_token             TEXT,
    datos_biometricos     TEXT,
    sesion_activa         BOOLEAN   NOT NULL DEFAULT FALSE,
    fecha_ultimo_acceso   TIMESTAMP,
    fecha_creacion        TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_logins_cliente UNIQUE (cliente_id),
    CONSTRAINT fk_logins_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id)
);

CREATE INDEX IF NOT EXISTS ix_logins_sesion_activa ON logins (sesion_activa);
