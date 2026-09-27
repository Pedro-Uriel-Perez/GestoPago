-- password_hash NO pasa por AesStringConverter: es un hash de un solo
-- sentido (BCrypt), no un dato cifrado reversible. Cifrarlo ademas con AES
-- no aportaria seguridad adicional y complicaria la verificacion.
ALTER TABLE logins
    ADD COLUMN IF NOT EXISTS password_hash TEXT;
