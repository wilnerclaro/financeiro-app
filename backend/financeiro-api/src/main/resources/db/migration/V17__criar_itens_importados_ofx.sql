CREATE TABLE itens_importados_ofx (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL,
    conta_id UUID NOT NULL,
    identificador VARCHAR(64) NOT NULL,
    lancamento_id UUID NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_itens_importados_ofx_conta UNIQUE (usuario_id, conta_id, identificador),
    CONSTRAINT fk_itens_importados_ofx_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_itens_importados_ofx_conta FOREIGN KEY (conta_id) REFERENCES contas(id),
    CONSTRAINT fk_itens_importados_ofx_lancamento FOREIGN KEY (lancamento_id) REFERENCES lancamentos_financeiros(id)
);
CREATE TRIGGER trg_itens_importados_ofx_definir_atualizado_em
    BEFORE UPDATE ON itens_importados_ofx
    FOR EACH ROW EXECUTE FUNCTION definir_atualizado_em();
