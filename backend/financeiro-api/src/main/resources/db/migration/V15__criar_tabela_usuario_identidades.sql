ALTER TABLE usuarios ALTER COLUMN senha_hash DROP NOT NULL;

CREATE TABLE usuario_identidades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL,
    provedor VARCHAR(20) NOT NULL,
    sujeito VARCHAR(255) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_usuario_identidades_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT uk_usuario_identidades_provedor_sujeito
        UNIQUE (provedor, sujeito),
    CONSTRAINT ck_usuario_identidades_provedor
        CHECK (provedor IN ('GOOGLE'))
);

CREATE INDEX idx_usuario_identidades_usuario_id ON usuario_identidades (usuario_id);

CREATE TRIGGER trg_usuario_identidades_definir_atualizado_em
    BEFORE UPDATE ON usuario_identidades
    FOR EACH ROW
    EXECUTE FUNCTION definir_atualizado_em();
