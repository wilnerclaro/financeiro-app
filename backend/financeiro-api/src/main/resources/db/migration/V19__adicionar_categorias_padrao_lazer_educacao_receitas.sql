INSERT INTO categorias_padrao (categoria_pai_id, nome, tipo, fonte_receita)
SELECT grupo.id, item.nome, grupo.tipo, grupo.fonte_receita
FROM (VALUES
        ('Lazer', 'DESPESA', 'Passeios'),
        ('Lazer', 'DESPESA', 'Jogos'),
        ('Educação', 'DESPESA', 'Cursos'),
        ('Receitas passivas', 'RECEITA', 'Rendimento caixinha'),
        ('Receitas ativas', 'RECEITA', 'Pix recebido')
) AS item (grupo, tipo, nome)
JOIN categorias_padrao grupo
  ON grupo.categoria_pai_id IS NULL
 AND grupo.nome = item.grupo
 AND grupo.tipo = item.tipo
WHERE NOT EXISTS (
    SELECT 1
    FROM categorias_padrao sub
    WHERE sub.categoria_pai_id = grupo.id
      AND UPPER(sub.nome) = UPPER(item.nome)
);

INSERT INTO categorias (usuario_id, categoria_pai_id, nome, tipo, fonte_receita)
SELECT grupo.usuario_id, grupo.id, item.nome, grupo.tipo, grupo.fonte_receita
FROM (VALUES
        ('Lazer', 'DESPESA', 'Passeios'),
        ('Lazer', 'DESPESA', 'Jogos'),
        ('Educação', 'DESPESA', 'Cursos'),
        ('Receitas passivas', 'RECEITA', 'Rendimento caixinha'),
        ('Receitas ativas', 'RECEITA', 'Pix recebido'),
        ('Outros gastos', 'DESPESA', 'Eletrônicos')
) AS item (grupo, tipo, nome)
JOIN categorias grupo
  ON grupo.categoria_pai_id IS NULL
 AND grupo.ativa
 AND UPPER(grupo.nome) = UPPER(item.grupo)
 AND grupo.tipo = item.tipo
WHERE NOT EXISTS (
    SELECT 1
    FROM categorias sub
    WHERE sub.categoria_pai_id = grupo.id
      AND UPPER(sub.nome) = UPPER(item.nome)
);
