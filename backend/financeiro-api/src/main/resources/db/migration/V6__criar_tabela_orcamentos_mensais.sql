CREATE TABLE orcamentos_mensais (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    usuario_id UUID NOT NULL,
                                    categoria_id UUID NOT NULL,
                                    ano INTEGER NOT NULL,
                                    mes INTEGER NOT NULL,
                                    tipo VARCHAR(20) NOT NULL,
                                    valor_previsto NUMERIC(19, 2) NOT NULL,
                                    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT fk_orcamentos_mensais_usuario
                                        FOREIGN KEY (usuario_id)
                                            REFERENCES usuarios (id),

                                    CONSTRAINT fk_orcamentos_mensais_categoria
                                        FOREIGN KEY (categoria_id)
                                            REFERENCES categorias (id),

                                    CONSTRAINT ck_orcamentos_mensais_ano
                                        CHECK (ano >= 2000),

                                    CONSTRAINT ck_orcamentos_mensais_mes
                                        CHECK (mes BETWEEN 1 AND 12),

                                    CONSTRAINT ck_orcamentos_mensais_tipo
                                        CHECK (tipo IN ('RECEITA', 'DESPESA')),

                                    CONSTRAINT ck_orcamentos_mensais_valor_previsto
                                        CHECK (valor_previsto >= 0),

                                    CONSTRAINT uk_orcamentos_mensais_usuario_categoria_ano_mes
                                        UNIQUE (usuario_id, categoria_id, ano, mes)
);

CREATE INDEX idx_orcamentos_mensais_usuario_id ON orcamentos_mensais (usuario_id);
CREATE INDEX idx_orcamentos_mensais_categoria_id ON orcamentos_mensais (categoria_id);
CREATE INDEX idx_orcamentos_mensais_ano_mes ON orcamentos_mensais (ano, mes);
CREATE INDEX idx_orcamentos_mensais_tipo ON orcamentos_mensais (tipo);

CREATE TRIGGER trg_orcamentos_mensais_definir_atualizado_em
    BEFORE UPDATE ON orcamentos_mensais
    FOR EACH ROW
    EXECUTE FUNCTION definir_atualizado_em();