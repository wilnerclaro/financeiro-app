ALTER TABLE itens_importados_ofx RENAME TO itens_importados;
ALTER INDEX itens_importados_ofx_pkey RENAME TO itens_importados_pkey;
ALTER TABLE itens_importados RENAME CONSTRAINT uk_itens_importados_ofx_conta TO uk_itens_importados_conta;
ALTER TABLE itens_importados RENAME CONSTRAINT fk_itens_importados_ofx_usuario TO fk_itens_importados_usuario;
ALTER TABLE itens_importados RENAME CONSTRAINT fk_itens_importados_ofx_conta TO fk_itens_importados_conta;
ALTER TABLE itens_importados RENAME CONSTRAINT fk_itens_importados_ofx_lancamento TO fk_itens_importados_lancamento;
ALTER TRIGGER trg_itens_importados_ofx_definir_atualizado_em ON itens_importados
    RENAME TO trg_itens_importados_definir_atualizado_em;
ALTER TABLE itens_importados ADD COLUMN formato VARCHAR(3) NOT NULL DEFAULT 'OFX';
ALTER TABLE itens_importados ALTER COLUMN formato DROP DEFAULT;
ALTER TABLE itens_importados
    ADD CONSTRAINT ck_itens_importados_formato CHECK (formato IN ('OFX', 'CSV', 'PDF'));
