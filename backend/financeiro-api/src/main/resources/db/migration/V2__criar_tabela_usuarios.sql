CREATE TABLE usuarios (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          nome VARCHAR(120) NOT NULL,
                          email VARCHAR(180) NOT NULL,
                          senha_hash VARCHAR(255) NOT NULL,
                          ativo BOOLEAN NOT NULL DEFAULT TRUE,
                          criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT uk_usuarios_email UNIQUE (email)
);

CREATE TRIGGER trg_usuarios_definir_atualizado_em
    BEFORE UPDATE ON usuarios
    FOR EACH ROW
    EXECUTE FUNCTION definir_atualizado_em();