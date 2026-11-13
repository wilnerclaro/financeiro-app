ALTER TABLE categorias
    DROP CONSTRAINT IF EXISTS uk_categorias_usuario_nome_tipo_pai;

CREATE UNIQUE INDEX uk_categorias_usuario_nome_tipo_sem_pai
    ON categorias (usuario_id, UPPER(nome), tipo)
    WHERE categoria_pai_id IS NULL;

CREATE UNIQUE INDEX uk_categorias_usuario_nome_tipo_com_pai
    ON categorias (usuario_id, UPPER(nome), tipo, categoria_pai_id)
    WHERE categoria_pai_id IS NOT NULL;
