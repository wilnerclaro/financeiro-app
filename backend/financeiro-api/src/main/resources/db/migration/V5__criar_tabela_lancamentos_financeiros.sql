CREATE TABLE lancamentos_financeiros (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                         usuario_id UUID NOT NULL,
                                         conta_id UUID NOT NULL,
                                         categoria_id UUID NOT NULL,
                                         descricao VARCHAR(180) NOT NULL,
                                         tipo VARCHAR(20) NOT NULL,
                                         valor NUMERIC(19, 2) NOT NULL,
                                         data_competencia DATE NOT NULL,
                                         data_vencimento DATE NULL,
                                         data_pagamento DATE NULL,
                                         status VARCHAR(20) NOT NULL,
                                         observacao TEXT NULL,
                                         criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                         CONSTRAINT fk_lancamentos_financeiros_usuario
                                             FOREIGN KEY (usuario_id)
                                                 REFERENCES usuarios (id),

                                         CONSTRAINT fk_lancamentos_financeiros_conta
                                             FOREIGN KEY (conta_id)
                                                 REFERENCES contas (id),

                                         CONSTRAINT fk_lancamentos_financeiros_categoria
                                             FOREIGN KEY (categoria_id)
                                                 REFERENCES categorias (id),

                                         CONSTRAINT ck_lancamentos_financeiros_tipo
                                             CHECK (tipo IN ('RECEITA', 'DESPESA')),

                                         CONSTRAINT ck_lancamentos_financeiros_status
                                             CHECK (status IN ('PENDENTE', 'PAGO', 'ATRASADO', 'CANCELADO')),

                                         CONSTRAINT ck_lancamentos_financeiros_valor
                                             CHECK (valor > 0)
);

CREATE INDEX idx_lancamentos_financeiros_usuario_id ON lancamentos_financeiros (usuario_id);
CREATE INDEX idx_lancamentos_financeiros_conta_id ON lancamentos_financeiros (conta_id);
CREATE INDEX idx_lancamentos_financeiros_categoria_id ON lancamentos_financeiros (categoria_id);
CREATE INDEX idx_lancamentos_financeiros_tipo ON lancamentos_financeiros (tipo);
CREATE INDEX idx_lancamentos_financeiros_status ON lancamentos_financeiros (status);
CREATE INDEX idx_lancamentos_financeiros_data_competencia ON lancamentos_financeiros (data_competencia);
CREATE INDEX idx_lancamentos_financeiros_data_vencimento ON lancamentos_financeiros (data_vencimento);
CREATE INDEX idx_lancamentos_financeiros_data_pagamento ON lancamentos_financeiros (data_pagamento);

CREATE TRIGGER trg_lancamentos_financeiros_definir_atualizado_em
    BEFORE UPDATE ON lancamentos_financeiros
    FOR EACH ROW
    EXECUTE FUNCTION definir_atualizado_em();