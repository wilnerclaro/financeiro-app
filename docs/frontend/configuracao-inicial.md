# Configuração inicial

Fluxo aprovado em 16/11/2026: objetivo de independência financeira, orçamento
previsto, contas e primeiro lançamento. A configuração é privada por usuário
e persiste no servidor para permitir retomada entre acessos.

## Jornada

1. Boas-vindas apresenta as quatro etapas.
2. Objetivo registra a renda passiva mensal desejada, maior que zero.
3. Orçamento carrega o previsto e o realizado da competência escolhida.
   Alterações usam o contrato existente de orçamento, preservando itens que
   não foram editados. Preencher o previsto não cria lançamentos.
   A tela mostra uma aba por vez (Receitas ou Despesas), tem busca por nome
   (sem diferenciar acento) e cada grupo abre e fecha mostrando o total
   previsto e o realizado. O resumo com receitas, despesas e sobra previstas
   fica fixo no rodapé enquanto a lista rola (pedido do Wilner em 23/11/2026).
4. Contas reaproveita contas ativas existentes e permite cadastrar outra.
   Saldo inicial não é receita; cartão mantém fechamento e vencimento.
5. Primeiro lançamento abre o formulário existente, com a opção de concluir
   sem registrar movimento. Compra no cartão continua pendente na fatura.
6. Conclusão permite voltar ao início e revisar a configuração.

Ao abrir o início, uma configuração incompleta e não pausada direciona para
`/configuracao-inicial`. A ação de continuar depois grava a pausa; o início
passa a oferecer retomada. A configuração concluída não redireciona novamente.
O usuário pode continuar acessando as demais áreas pelo menu.

## Objetivo e Independenciômetro

A renda passiva desejada é uma referência declarada pelo usuário. Não substitui
o denominador do Independenciômetro, que continua sendo a despesa realizada.
O fluxo não calcula patrimônio necessário, prazo ou taxa de rentabilidade.

## Persistência

`GET /api/configuracao-inicial` consulta o estado do usuário autenticado.
`PUT /api/configuracao-inicial` grava `rendaPassivaDesejada`, `etapa`, `pausado`,
`ano` e `mes`. O identificador do usuário vem do token.

As etapas são `BOAS_VINDAS`, `OBJETIVO`, `ORCAMENTO`, `CONTAS`, `LANCAMENTO`
e `CONCLUIDO`. Sem registro, o servidor devolve boas-vindas e o mês atual.
Contas, orçamento e lançamentos continuam utilizando seus próprios endpoints.

Salvar o primeiro lançamento e concluir a configuração são duas operações.
Se a segunda falhar, o formulário confirma o lançamento já salvo e oferece
uma nova tentativa somente para concluir a configuração, sem duplicar o
movimento financeiro.
