CREATE TABLE categorias (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            usuario_id UUID NOT NULL,
                            categoria_pai_id UUID NULL,
                            nome VARCHAR(100) NOT NULL,
                            tipo VARCHAR(20) NOT NULL,
                            ativa BOOLEAN NOT NULL DEFAULT TRUE,
                            criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT fk_categorias_usuario
                                FOREIGN KEY (usuario_id)
                                    REFERENCES usuarios (id),

                            CONSTRAINT fk_categorias_categoria_pai
                                FOREIGN KEY (categoria_pai_id)
                                    REFERENCES categorias (id),

                            CONSTRAINT ck_categorias_tipo
                                CHECK (tipo IN ('RECEITA', 'DESPESA')),

                            CONSTRAINT uk_categorias_usuario_nome_tipo_pai
                                UNIQUE (usuario_id, nome, tipo, categoria_pai_id)
);

CREATE INDEX idx_categorias_usuario_id ON categorias (usuario_id);
CREATE INDEX idx_categorias_categoria_pai_id ON categorias (categoria_pai_id);
CREATE INDEX idx_categorias_tipo ON categorias (tipo);
CREATE INDEX idx_categorias_ativa ON categorias (ativa);

CREATE TRIGGER trg_categorias_definir_atualizado_em
    BEFORE UPDATE ON categorias
    FOR EACH ROW
    EXECUTE FUNCTION definir_atualizado_em();