CREATE TABLE configuracoes_iniciais (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL,
    renda_passiva_desejada NUMERIC(19,2),
    etapa VARCHAR(20) NOT NULL,
    pausado BOOLEAN NOT NULL,
    ano INTEGER NOT NULL,
    mes INTEGER NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_configuracoes_iniciais_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT uk_configuracoes_iniciais_usuario UNIQUE (usuario_id),
    CONSTRAINT ck_configuracoes_iniciais_objetivo CHECK (renda_passiva_desejada > 0),
    CONSTRAINT ck_configuracoes_iniciais_etapa
        CHECK (etapa IN ('BOAS_VINDAS', 'OBJETIVO', 'ORCAMENTO', 'CONTAS', 'LANCAMENTO', 'CONCLUIDO')),
    CONSTRAINT ck_configuracoes_iniciais_competencia CHECK (ano BETWEEN 2000 AND 2100 AND mes BETWEEN 1 AND 12)
);

CREATE TRIGGER trg_configuracoes_iniciais_definir_atualizado_em
    BEFORE UPDATE ON configuracoes_iniciais
    FOR EACH ROW
    EXECUTE FUNCTION definir_atualizado_em();
