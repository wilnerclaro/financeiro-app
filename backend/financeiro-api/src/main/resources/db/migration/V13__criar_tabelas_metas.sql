CREATE TABLE metas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL,
    nome VARCHAR(120) NOT NULL,
    valor_necessario NUMERIC(19, 2) NOT NULL,
    investimento_inicial NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_metas_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_metas_valor_necessario
        CHECK (valor_necessario > 0),
    CONSTRAINT ck_metas_investimento_inicial
        CHECK (investimento_inicial >= 0)
);

CREATE UNIQUE INDEX uk_metas_usuario_nome ON metas (usuario_id, LOWER(nome));

CREATE TRIGGER trg_metas_definir_atualizado_em
    BEFORE UPDATE ON metas
    FOR EACH ROW
    EXECUTE FUNCTION definir_atualizado_em();

CREATE TABLE movimentos_metas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meta_id UUID NOT NULL,
    ano INTEGER NOT NULL,
    mes INTEGER NOT NULL,
    aporte NUMERIC(19, 2) NOT NULL DEFAULT 0,
    juros NUMERIC(19, 2) NOT NULL DEFAULT 0,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_movimentos_metas_meta
        FOREIGN KEY (meta_id) REFERENCES metas (id),
    CONSTRAINT ck_movimentos_metas_ano
        CHECK (ano >= 2000),
    CONSTRAINT ck_movimentos_metas_mes
        CHECK (mes BETWEEN 1 AND 12),
    CONSTRAINT uk_movimentos_metas_meta_ano_mes
        UNIQUE (meta_id, ano, mes)
);

CREATE TRIGGER trg_movimentos_metas_definir_atualizado_em
    BEFORE UPDATE ON movimentos_metas
    FOR EACH ROW
    EXECUTE FUNCTION definir_atualizado_em();
