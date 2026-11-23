ALTER TABLE categorias ADD COLUMN fonte_receita VARCHAR(10) NULL;

UPDATE categorias SET fonte_receita = 'ATIVA' WHERE tipo = 'RECEITA';

ALTER TABLE categorias ADD CONSTRAINT ck_categorias_fonte_receita
    CHECK (
        (tipo = 'RECEITA' AND fonte_receita IN ('ATIVA', 'PASSIVA'))
        OR (tipo = 'DESPESA' AND fonte_receita IS NULL)
    );
