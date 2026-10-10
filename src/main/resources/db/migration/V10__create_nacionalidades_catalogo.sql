-- Catalogo de nacionalidades: antes "nacionalidad" en clientes era TEXT
-- libre, sin ninguna lista de valores validos. Se normaliza a una tabla
-- catalogo (patron estandar para listas fijas de opciones), y clientes
-- pasa a referenciarla por FK en vez de guardar el texto directo.
CREATE TABLE IF NOT EXISTS nacionalidades (
    id      SERIAL PRIMARY KEY,
    nombre  TEXT NOT NULL UNIQUE
);

INSERT INTO nacionalidades (nombre) VALUES
    ('MEXICANA'),
    ('ESTADOUNIDENSE'),
    ('CANADIENSE'),
    ('ESPANOLA'),
    ('COLOMBIANA'),
    ('ARGENTINA'),
    ('GUATEMALTECA'),
    ('CHINA'),
    ('FRANCESA'),
    ('ALEMANA'),
    ('BRASILENA'),
    ('CUBANA'),
    ('VENEZOLANA'),
    ('OTRA')
ON CONFLICT (nombre) DO NOTHING;

ALTER TABLE clientes ADD COLUMN IF NOT EXISTS nacionalidad_id INTEGER;

-- Reasigna los clientes existentes al catalogo por coincidencia de nombre;
-- lo que no coincide (datos de prueba con texto libre) cae en 'OTRA'.
UPDATE clientes c
SET nacionalidad_id = n.id
FROM nacionalidades n
WHERE UPPER(c.nacionalidad) = n.nombre
  AND c.nacionalidad_id IS NULL;

UPDATE clientes
SET nacionalidad_id = (SELECT id FROM nacionalidades WHERE nombre = 'OTRA')
WHERE nacionalidad_id IS NULL;

ALTER TABLE clientes
    ALTER COLUMN nacionalidad_id SET NOT NULL,
    ADD CONSTRAINT fk_clientes_nacionalidad FOREIGN KEY (nacionalidad_id) REFERENCES nacionalidades (id),
    DROP COLUMN nacionalidad;
