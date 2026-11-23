CREATE TABLE categorias_padrao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    categoria_pai_id UUID NULL,
    nome VARCHAR(100) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    fonte_receita VARCHAR(10) NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_categorias_padrao_categoria_pai
        FOREIGN KEY (categoria_pai_id)
            REFERENCES categorias_padrao (id),

    CONSTRAINT ck_categorias_padrao_tipo
        CHECK (tipo IN ('RECEITA', 'DESPESA')),

    CONSTRAINT ck_categorias_padrao_fonte_receita
        CHECK (
            (tipo = 'RECEITA' AND fonte_receita IN ('ATIVA', 'PASSIVA'))
            OR (tipo = 'DESPESA' AND fonte_receita IS NULL)
        )
);

CREATE UNIQUE INDEX uk_categorias_padrao_nome_tipo_sem_pai
    ON categorias_padrao (UPPER(nome), tipo)
    WHERE categoria_pai_id IS NULL;

CREATE UNIQUE INDEX uk_categorias_padrao_nome_tipo_com_pai
    ON categorias_padrao (UPPER(nome), tipo, categoria_pai_id)
    WHERE categoria_pai_id IS NOT NULL;

CREATE TRIGGER trg_categorias_padrao_definir_atualizado_em
    BEFORE UPDATE ON categorias_padrao
    FOR EACH ROW
    EXECUTE FUNCTION definir_atualizado_em();

WITH grupo AS (
    INSERT INTO categorias_padrao (nome, tipo, fonte_receita)
    VALUES ('Receitas ativas', 'RECEITA', 'ATIVA')
    RETURNING id
)
INSERT INTO categorias_padrao (categoria_pai_id, nome, tipo, fonte_receita)
SELECT grupo.id, item.nome, 'RECEITA', 'ATIVA'
FROM grupo, (VALUES
        ('Salários'),
        ('Bolsa de estudos'),
        ('13º Salário'),
        ('Férias'),
        ('Participação de Lucros e Resultados (PLR)'),
        ('Prestação de Serviços'),
        ('Doações'),
        ('Bonificações'),
        ('Recebimento de Dívidas'),
        ('Outros (Pensão, restituição de IR e etc)')
) AS item (nome);

WITH grupo AS (
    INSERT INTO categorias_padrao (nome, tipo, fonte_receita)
    VALUES ('Receitas passivas', 'RECEITA', 'PASSIVA')
    RETURNING id
)
INSERT INTO categorias_padrao (categoria_pai_id, nome, tipo, fonte_receita)
SELECT grupo.id, item.nome, 'RECEITA', 'PASSIVA'
FROM grupo, (VALUES
        ('Aluguéis'),
        ('Dividendos'),
        ('Aposentadoria/Pensão Vitalícia'),
        ('Juro Sobre Capital Próprio'),
        ('Outros')
) AS item (nome);

WITH grupo AS (
    INSERT INTO categorias_padrao (nome, tipo, fonte_receita)
    VALUES ('Moradia', 'DESPESA', NULL)
    RETURNING id
)
INSERT INTO categorias_padrao (categoria_pai_id, nome, tipo, fonte_receita)
SELECT grupo.id, item.nome, 'DESPESA', NULL
FROM grupo, (VALUES
        ('Aluguel / Prestação'),
        ('Condomínio'),
        ('Conta de luz'),
        ('Conta de água'),
        ('Gás'),
        ('IPTU'),
        ('Internet'),
        ('Telefone fixo'),
        ('Telefone celular'),
        ('Reparos na casa / apartamento'),
        ('Seguro Residencial'),
        ('Alarme'),
        ('Decoração'),
        ('Faxineira / diarista'),
        ('Empregados'),
        ('Serviços Periódicos')
) AS item (nome);

WITH grupo AS (
    INSERT INTO categorias_padrao (nome, tipo, fonte_receita)
    VALUES ('Alimentação', 'DESPESA', NULL)
    RETURNING id
)
INSERT INTO categorias_padrao (categoria_pai_id, nome, tipo, fonte_receita)
SELECT grupo.id, item.nome, 'DESPESA', NULL
FROM grupo, (VALUES
        ('Supermercado'),
        ('Restaurante'),
        ('Hortifruti / Feira-livre'),
        ('Padaria')
) AS item (nome);

WITH grupo AS (
    INSERT INTO categorias_padrao (nome, tipo, fonte_receita)
    VALUES ('Transporte', 'DESPESA', NULL)
    RETURNING id
)
INSERT INTO categorias_padrao (categoria_pai_id, nome, tipo, fonte_receita)
SELECT grupo.id, item.nome, 'DESPESA', NULL
FROM grupo, (VALUES
        ('Combustível'),
        ('Manutenção do carro'),
        ('Pedágio'),
        ('Estacionamento'),
        ('IPVA/DPVAT'),
        ('Aluguel / Prestação'),
        ('Seguro do carro'),
        ('Vale transporte'),
        ('Táxi / Uber / 99')
) AS item (nome);

WITH grupo AS (
    INSERT INTO categorias_padrao (nome, tipo, fonte_receita)
    VALUES ('Educação', 'DESPESA', NULL)
    RETURNING id
)
INSERT INTO categorias_padrao (categoria_pai_id, nome, tipo, fonte_receita)
SELECT grupo.id, item.nome, 'DESPESA', NULL
FROM grupo, (VALUES
        ('Mensalidade'),
        ('Atividade Extra'),
        ('Livros'),
        ('Xerox'),
        ('Papelaria'),
        ('Uniforme'),
        ('Equipamentos / Utensílios'),
        ('Outros')
) AS item (nome);

WITH grupo AS (
    INSERT INTO categorias_padrao (nome, tipo, fonte_receita)
    VALUES ('Saúde & Beleza', 'DESPESA', NULL)
    RETURNING id
)
INSERT INTO categorias_padrao (categoria_pai_id, nome, tipo, fonte_receita)
SELECT grupo.id, item.nome, 'DESPESA', NULL
FROM grupo, (VALUES
        ('Plano de saúde'),
        ('Médicos / Dentistas / Fisioterapeuta'),
        ('Maquiagem / Perfumaria'),
        ('Salão de beleza / barbearia'),
        ('Farmácia')
) AS item (nome);

WITH grupo AS (
    INSERT INTO categorias_padrao (nome, tipo, fonte_receita)
    VALUES ('Lazer', 'DESPESA', NULL)
    RETURNING id
)
INSERT INTO categorias_padrao (categoria_pai_id, nome, tipo, fonte_receita)
SELECT grupo.id, item.nome, 'DESPESA', NULL
FROM grupo, (VALUES
        ('Academia'),
        ('Clube'),
        ('Sócio Torcedor'),
        ('Jornais / Revistas'),
        ('TV por assinatura'),
        ('Cinema'),
        ('Teatro'),
        ('Feiras')
) AS item (nome);

WITH grupo AS (
    INSERT INTO categorias_padrao (nome, tipo, fonte_receita)
    VALUES ('Vestuário', 'DESPESA', NULL)
    RETURNING id
)
INSERT INTO categorias_padrao (categoria_pai_id, nome, tipo, fonte_receita)
SELECT grupo.id, item.nome, 'DESPESA', NULL
FROM grupo, (VALUES
        ('Roupas'),
        ('Calçados'),
        ('Acessórios')
) AS item (nome);

WITH grupo AS (
    INSERT INTO categorias_padrao (nome, tipo, fonte_receita)
    VALUES ('Outros gastos', 'DESPESA', NULL)
    RETURNING id
)
INSERT INTO categorias_padrao (categoria_pai_id, nome, tipo, fonte_receita)
SELECT grupo.id, item.nome, 'DESPESA', NULL
FROM grupo, (VALUES
        ('Eletrodomésticos'),
        ('Eletrônicos'),
        ('Imposto de Renda'),
        ('Seguros de vida'),
        ('Conselho de classe'),
        ('Sindicato'),
        ('INSS'),
        ('Previdência Privada'),
        ('Presentes'),
        ('Doações')
) AS item (nome);

WITH grupo AS (
    INSERT INTO categorias_padrao (nome, tipo, fonte_receita)
    VALUES ('Pet', 'DESPESA', NULL)
    RETURNING id
)
INSERT INTO categorias_padrao (categoria_pai_id, nome, tipo, fonte_receita)
SELECT grupo.id, item.nome, 'DESPESA', NULL
FROM grupo, (VALUES
        ('Ração'),
        ('Pet shop'),
        ('Veterinária')
) AS item (nome);

WITH grupo AS (
    INSERT INTO categorias_padrao (nome, tipo, fonte_receita)
    VALUES ('Diversos', 'DESPESA', NULL)
    RETURNING id
)
INSERT INTO categorias_padrao (categoria_pai_id, nome, tipo, fonte_receita)
SELECT grupo.id, item.nome, 'DESPESA', NULL
FROM grupo, (VALUES
        ('Serviços esporádicos'),
        ('Demais gastos'),
        ('Gastos em dinheiro sem referências'),
        ('Serviços bancários/taxas de investimentos'),
        ('Serviços gerais'),
        ('Multa'),
        ('Outros')
) AS item (nome);
