CREATE TABLE contas (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        usuario_id UUID NOT NULL,
                        nome VARCHAR(100) NOT NULL,
                        tipo VARCHAR(30) NOT NULL,
                        saldo_inicial NUMERIC(19, 2) NOT NULL DEFAULT 0,
                        saldo_atual NUMERIC(19, 2) NOT NULL DEFAULT 0,
                        ativa BOOLEAN NOT NULL DEFAULT TRUE,
                        criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT fk_contas_usuario
                            FOREIGN KEY (usuario_id)
                                REFERENCES usuarios (id),

                        CONSTRAINT ck_contas_tipo
                            CHECK (tipo IN ('CONTA_CORRENTE', 'POUPANCA', 'DINHEIRO', 'INVESTIMENTO', 'OUTRA')),

                        CONSTRAINT uk_contas_usuario_nome
                            UNIQUE (usuario_id, nome)
);

CREATE INDEX idx_contas_usuario_id ON contas (usuario_id);
CREATE INDEX idx_contas_tipo ON contas (tipo);
CREATE INDEX idx_contas_ativa ON contas (ativa);

CREATE TRIGGER trg_contas_definir_atualizado_em
    BEFORE UPDATE ON contas
    FOR EACH ROW
    EXECUTE FUNCTION definir_atualizado_em();