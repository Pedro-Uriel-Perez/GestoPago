ALTER TABLE gestopago_productos
    ADD COLUMN IF NOT EXISTS tipo_referencia TEXT,
    ADD COLUMN IF NOT EXISTS precio TEXT;
