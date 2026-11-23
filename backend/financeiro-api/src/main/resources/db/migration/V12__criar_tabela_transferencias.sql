CREATE TABLE transferencias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL,
    conta_origem_id UUID NOT NULL,
    conta_destino_id UUID NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,
    data_transferencia DATE NOT NULL,
    descricao VARCHAR(180) NULL,
    cancelada BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transferencias_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_transferencias_conta_origem
        FOREIGN KEY (conta_origem_id) REFERENCES contas (id),
    CONSTRAINT fk_transferencias_conta_destino
        FOREIGN KEY (conta_destino_id) REFERENCES contas (id),
    CONSTRAINT ck_transferencias_valor
        CHECK (valor > 0),
    CONSTRAINT ck_transferencias_contas_diferentes
        CHECK (conta_origem_id <> conta_destino_id)
);

CREATE INDEX idx_transferencias_usuario_data ON transferencias (usuario_id, data_transferencia);
CREATE INDEX idx_transferencias_conta_origem_id ON transferencias (conta_origem_id);
CREATE INDEX idx_transferencias_conta_destino_id ON transferencias (conta_destino_id);

CREATE TRIGGER trg_transferencias_definir_atualizado_em
    BEFORE UPDATE ON transferencias
    FOR EACH ROW
    EXECUTE FUNCTION definir_atualizado_em();
