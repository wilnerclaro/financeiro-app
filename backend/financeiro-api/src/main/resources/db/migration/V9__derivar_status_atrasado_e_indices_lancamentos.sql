UPDATE lancamentos_financeiros SET status = 'PENDENTE' WHERE status = 'ATRASADO';

ALTER TABLE lancamentos_financeiros DROP CONSTRAINT ck_lancamentos_financeiros_status;
ALTER TABLE lancamentos_financeiros ADD CONSTRAINT ck_lancamentos_financeiros_status
    CHECK (status IN ('PENDENTE', 'PAGO', 'CANCELADO'));

ALTER TABLE lancamentos_financeiros ADD COLUMN data_compra DATE NULL;

CREATE INDEX idx_lancamentos_financeiros_usuario_competencia
    ON lancamentos_financeiros (usuario_id, data_competencia);
CREATE INDEX idx_lancamentos_financeiros_usuario_status_vencimento
    ON lancamentos_financeiros (usuario_id, status, data_vencimento);
