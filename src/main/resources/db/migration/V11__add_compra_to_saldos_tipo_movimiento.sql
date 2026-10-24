ALTER TABLE saldos DROP CONSTRAINT ck_saldos_tipo_movimiento;

ALTER TABLE saldos
    ADD CONSTRAINT ck_saldos_tipo_movimiento
        CHECK (tipo_movimiento IN ('APERTURA', 'DEPOSITO', 'RETIRO', 'AJUSTE', 'COMPRA'));
