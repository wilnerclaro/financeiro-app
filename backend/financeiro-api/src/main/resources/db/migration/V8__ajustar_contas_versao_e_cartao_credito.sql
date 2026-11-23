ALTER TABLE contas ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;

ALTER TABLE contas ADD COLUMN dia_fechamento INTEGER NULL;
ALTER TABLE contas ADD COLUMN dia_vencimento INTEGER NULL;

ALTER TABLE contas DROP CONSTRAINT ck_contas_tipo;
ALTER TABLE contas ADD CONSTRAINT ck_contas_tipo
    CHECK (tipo IN ('CONTA_CORRENTE', 'POUPANCA', 'DINHEIRO', 'INVESTIMENTO', 'CARTAO_CREDITO', 'OUTRA'));

ALTER TABLE contas ADD CONSTRAINT ck_contas_dias_fatura
    CHECK (
        (tipo = 'CARTAO_CREDITO'
            AND dia_fechamento BETWEEN 1 AND 28
            AND dia_vencimento BETWEEN 1 AND 28)
        OR (tipo <> 'CARTAO_CREDITO' AND dia_fechamento IS NULL AND dia_vencimento IS NULL)
    );
