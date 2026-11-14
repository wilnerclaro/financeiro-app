---
name: glossario-financeiro
description: Glossário de domínio e regras de cálculo do financeiro-app, extraídos da planilha de origem. Use ao modelar entidades, implementar qualquer cálculo financeiro, ou escrever testes de valor esperado.
---

# Glossário e regras de cálculo

Extraído de `M4A2_PlanilhaDoMira_MCN1T2-V5_7.xlsx`. Cada regra traz a célula de
origem para conferência.

**Autoridade**: este documento é a fonte canônica. Se uma regra não está aqui,
ela não foi verificada — **pare e sinalize a lacuna**, não improvise. Regra
financeira inventada é o pior defeito possível neste projeto.

---

## Convenções gerais da planilha

**Guarda de divisão por zero.** Praticamente todo percentual é escrito como
`IF(divisor = 0; 0; conta)`. O resultado esperado quando o divisor é zero é
**0, não erro e não nulo**. Vale para percentual na carteira, variação mensal e
crescimento.

Exceções (ver "Defeitos conhecidos"): `Evolução das Receitas` e
`Evolução dos Gastos` não têm essa guarda. **Na implementação, aplique a guarda
também nelas.**

**Previsto x Realizado.** Receitas, despesas e orçamento existem sempre em par.
Nunca colapse em um campo só. Na planilha, PREVISTO e REALIZADO são colunas
irmãs por mês.

**Competência mensal.** Tudo é organizado por mês do ano civil. Na planilha
isso vira 12 blocos de colunas; **no banco isso é sempre uma linha por
(entidade, competência)**. Nunca replique colunas mensais no schema.

**Escala monetária.** Padrão `NUMERIC(19,2)`. Exceções obrigatórias:
- Quantidade de cripto: a planilha usa `0,005 BTC` → mínimo `NUMERIC(30,10)`
- Cotação: `NUMERIC(19,6)`
- Percentuais armazenados: fração (0..1), não 0..100. A planilha guarda `0,5422`
  e formata como 54,22%.

---

## Módulo: Controle Financeiro Pessoal

*Este é o módulo em construção. Mapeado integralmente.*

### Receitas — duas naturezas

As receitas se dividem em dois blocos, e a distinção é central:

**FONTE ATIVA** (linhas 5-15): Salários, Bolsa de estudos, 13º Salário, Férias,
Participação de Lucros e Resultados (PLR), Prestação de Serviços, Doações,
Bonificações, Resgate de Investimentos, Recebimento de Dívidas, Outros (Pensão,
restituição de IR e etc). Renda que exige trabalho.

> **Resgate de Investimentos (linha 13) não vira categoria no app.** Tirar
> dinheiro de um investimento é transferência entre contas (`/api/transferencias`),
> não receita: lançado como receita, contaria duas vezes e inflaria o "entrou",
> o SALDO [C177] e as metas de 30%/50%. Decisão registrada em 13/11/2026.

**FONTE PASSIVA** (linhas 18-22): Aluguéis, Dividendos, Aposentadoria/Pensão
Vitalícia, Juro Sobre Capital Próprio, Outros. Renda que não exige trabalho.

```
TOTAL FONTE ATIVA   = SUM(linhas 5:15)          [C16]
TOTAL FONTE PASSIVA = SUM(linhas 18:22)         [C23]
TOTAL GERAL         = ativa + passiva           [C24 = C23 + C16]
```

> **No código**: `categorias.fonte_receita` (`ATIVA`/`PASSIVA`, V10). Receita
> sem fonte vira `ATIVA`; subcategoria herda a fonte do grupo; despesa não tem
> fonte. No cadastro o usuário recebe os grupos "Receitas ativas" e "Receitas
> passivas" com os itens acima, copiados da tabela `categorias_padrao` (V14).

### Despesas — hierarquia de dois níveis

Grupos, cada um somando seus subitens:

| Grupo | Célula |
|---|---|
| Moradia | `C53 = SUM(C54:C69)` |
| Alimentação | `C70 = SUM(C71:C74)` |
| Transporte | `C75 = SUM(C76:C84)` |
| Educação | `C85 = SUM(C86:C93)` |
| Saúde & Beleza | `C94 = SUM(C95:C99)` |
| Lazer | `C100 = SUM(C101:C108)` |
| Vestuário | `C109 = SUM(C110:C112)` |
| Outros gastos | `C113 = SUM(C114:C123)` |
| Pet | `C124 = SUM(C125:C127)` |
| Diversos | `C128 = SUM(C129:C135)` |

```
TOTAL DESPESAS = soma dos 10 grupos             [C136, C151]
```

Exemplos de subitens (Moradia): Aluguel/Prestação, Condomínio, Conta de luz,
Conta de água, Gás, IPTU, Internet, Telefone fixo, Telefone celular, Reparos,
Seguro Residencial, Alarme, Decoração, Faxineira/diarista, Empregados,
Serviços Periódicos.

> **Isso mapeia exatamente na tabela `categorias` existente**, com
> `categoria_pai_id` e a restrição de dois níveis já implementada em
> `CategoriaService.buscarCategoriaPai`. Grupo = categoria pai,
> subitem = subcategoria.

### INDEPENDENCIÔMETRO REAL

A métrica central da planilha.

```
independenciometro(mês) = receita_passiva_REALIZADA / despesa_total_REALIZADA
```

Origem: `C201 = D23/D151`. Note que usa as colunas REALIZADO (D), não PREVISTO.

Interpretação: quanto da despesa já é coberto por renda passiva. Valor `1,0`
significa independência financeira. É o número que motiva a planilha inteira —
trate-o como requisito de primeira classe, não como métrica secundária.

**Casos de borda**: despesa zero no mês → aplicar guarda, retornar 0 (a
planilha não protege esta divisão; ver defeitos).

**Exibição (tela Início, `InicioService`)** — regras de UX, não da planilha:
faixas 0 / 0,10 / 0,20 / 0,30 / 0,50 (a faixa é a maior ≤ índice); próximo
marco é o primeiro de 0,10 / 0,20 / 0,30 / 0,50 / 1,00 acima do índice (nulo
se ≥ 1); "falta" = `despesa × marco − passiva`, mínimo 0, em reais por mês.

Valores de teste: passiva 1000, despesa 3000 → índice 0,3333, faixa 0,30,
próximo marco 0,50, falta 500,00.

### Economizado

```
SALDO           = receita_total_REALIZADA − despesa_total_REALIZADA   [C177 = D24 − D136]
30% DO SALÁRIO  = receita_total_REALIZADA × 0,30                      [C178]
50% DO SALÁRIO  = receita_total_REALIZADA × 0,50                      [C179]
```

As duas últimas são linhas de referência para comparação visual com o saldo.
No app: meta atingida quando `SALDO ≥ valor da meta` e houve receita; "falta"
= `meta − SALDO`, mínimo 0. Valores de teste: entrou 5000, saiu 3000 → sobrou
2000, meta 30% = 1500 (atingiu), meta 50% = 2500 (falta 500).

### Evolução mês a mês

```
evolução_receitas(m) = (total_m − total_m-1) / total_m-1     [F25]
evolução_gastos(m)   = (total_m − total_m-1) / total_m-1     [F152]
```

Ambas sobre valores REALIZADOS. **Sem guarda de divisão por zero na planilha —
adicione na implementação.**

### Totais e médias por linha

```
TOTAL PREVISTO  = soma dos 12 previstos          [AA5]
TOTAL REALIZADO = soma dos 12 realizados         [AB5]
MÉDIA PREVISTO  = AVERAGE dos 12 previstos       [AC5]
MÉDIA REALIZADO = AVERAGE dos 12 realizados      [AD5]
```

`AVERAGE` do Excel **ignora células vazias**, e retorna `#DIV/0!` quando todas
estão vazias (visível na planilha atual). Na implementação: média sobre os
meses **com valor informado**; se nenhum, retornar 0 — não erro.

### Comparativo Projetado x Realizado

Linhas 28/29 e 155/156 apenas reorganizam PREVISTO e REALIZADO lado a lado por
mês, para gráfico. Não é cálculo novo.

---

## Módulo: Carteira de Investimentos

*Não implementado. Mapeado — implementar quando chegar a vez.*

Cinco classes compartilham **um único modelo de posição mensal**:
Renda Fixa, Ações, FII, Criptomoedas, Imobilizado.

- **Ações e FII são idênticas**, fórmula por fórmula
- **Renda Fixa** é o caso degenerado: só valor, sem quantidade/cotação/beta
- **Criptomoedas**: quantidade × cotação, sem beta
- **Imobilizado**: valor arrastado mês a mês, sem quantidade

### Fórmulas

```
TOTAL do ativo        = cotação × quantidade              [AÇÕES H5 = G5*E5]
```
Atenção: valor de **mercado** (cotação), não custo (preço médio).

```
% na carteira         = IF(total_mês = 0; 0; total_ativo / total_mês)   [J5]
variação mensal       = IF(anterior = 0; 0; (atual − anterior)/anterior) [T5]
crescimento sobre PM  = (cotação − preço_médio) × quantidade            [EE5]
```
O último é o lucro **não realizado** da posição.

```
crescimento esperado mensal = (taxa_anual + 1)^(1/12) − 1               [I5]
```
Composta, não linear.

```
beta ponderado da carteira = Σ(% na carteira × beta do ativo)           [K45]
```

```
crescimento realizado = (valor_dezembro − valor_base) / valor_base      [ED5]
```

**`valor_base` NÃO é janeiro.** É o **primeiro mês com valor diferente de
zero** na linha do ativo — o mês em que ele entrou na carteira. Fórmula
matricial em `O51`. Implementar como "primeiro valor não-zero da série".
Usar janeiro produz resultado errado para qualquer ativo comprado no meio do
ano.

### Aportes — regra crítica de rentabilidade

```
TOTAL COM APORTES       = soma das classes                    [CONSOLIDADO D10]
APORTES MENSAIS         = novos aportes do mês                [D11]
CRESCIMENTO SEM APORTES = total − aportes                     [D12 = D10 − D11]
```

Rentabilidade calculada sem descontar aporte novo faz o aporte parecer
rendimento. **Toda métrica de rentabilidade usa a base sem aportes.**

### Hedge (Ações e FII)

```
exposição a proteger = total_da_classe × beta_ponderado       [H48]
mini contratos WINFUT = (exposição / cotação_winfut) × 5      [J48]
```

---

## Módulo: Trade

*Não implementado. Mapeado.*

**Este módulo é baseado em eventos**, ao contrário da carteira (posição
mensal). São dois modelos distintos convivendo — não unifique.

### Trade de ações

```
TOTAL (entrada)  = quantidade × valor_compra + custos         [G4]
% loss           = IF(entrada = 0; 0; (stop_loss/entrada) − 1) [K4]
% gain           = IF(entrada = 0; 0; (stop_gain/entrada) − 1) [L4]
```

Operações encerradas (blocos mensais):
```
TOTAL saída  = (qtd_venda × valor_venda) − custos_venda       [L26]
RESULTADO    = IF(qtd = 0; 0; total_saída − total_entrada)    [M26]
rentabilidade da operação = IF(saída = 0; 0; (saída/entrada) − 1) [N26]
```

Resumo mensal e anual:
```
resultado_mês        = Σ resultados das operações do mês       [M24]
rentabilidade_mês    = Σ resultados / Σ investido              [N24]
rentabilidade_ano    = ((Σ resultados + Σ proventos) / Σ investido) − 1  [I12]
```

**PROVENTOS entram na rentabilidade.** Dividendos e JCP recebidos no período
compõem o resultado — ignorá-los subestima o retorno.

### Trade de opções

Estruturas multi-perna por ativo-objeto (BBDC4, B3SA3, BTOW3, JBSS3, MGLU3...).
Cada estrutura tem montagem e desmontagem.

```
TOTAL da perna = quantidade × prêmio                          [G5 = F5*E5]
```

**O sinal da quantidade codifica a direção**: negativa = ponta vendida,
positiva = comprada (ex.: `F21 = -1000`). Não modele com quantidade sempre
positiva e um campo de direção separado — as fórmulas de resultado quebram.

```
montagem líquida    = Σ(pernas) − custos                      [G9]
desmontagem líquida = Σ(pernas) − custos                      [M9]
RESULTADO           = montagem + desmontagem                  [N8]
classificação       = resultado > 0 ? "GAIN" : "LOSS"         [N7]
percentual          = resultado / (montagem × −1)             [N9]
```

---

## Módulo: Custo de Operações

*Não implementado. Mapeado.*

Custos fixos do período: Taxa de Liquidação, Taxa de Registro, Emolumentos, ISS.
```
CUSTO TOTAL DO PERÍODO = soma das quatro taxas                [D8]
```

Rateio proporcional entre as operações:
```
percentual da operação = total_operação / total_geral         [G12]
custo rateado          = custo_total_período × percentual     [H12]
CUSTO TOTAL da operação = corretagem + custo_rateado          [J12]
```

Colunas: ATIVOS, QUANTIDADE, PREÇO, TOTAL, PERCENTUAL, CUSTO PERCENTUAL,
CORRETAGEM, CUSTO TOTAL.

---

## Módulo: Planejamento e Metas

Aba **PLANEJAMENTO e METAS**, linhas 4 a 19 (até 12 metas por ano).

| Coluna | Campo | No app |
|---|---|---|
| B | META / SONHO | `metas.nome` |
| C | NECESSIDADE | `metas.valor_necessario` (> 0) |
| D | INVESTIMENTO | `metas.investimento_inicial` (≥ 0) |
| F, H, J… AB | JUROS/VARIAÇÃO do mês | `movimentos_metas.juros` (pode ser negativo) |
| G, I, K… AC | APORTE do mês | `movimentos_metas.aporte` (negativo = resgate) |
| E | ACUMULADO | calculado |

```
ACUMULADO   = INVESTIMENTO + Σ(juros + aportes)          [E7 = D7 + SUM(F7:AC7)]
TOTAL       = soma por coluna das metas                  [linha 19: C19, E19, F19…AC19]
```

A planilha soma só os 12 meses de um ano; no app o acumulado soma **todos** os
meses já registrados (a meta atravessa anos) e o painel mostra os 12 meses do
ano pedido. Percentual atingido = acumulado / necessidade (guarda → 0) e
"falta" = necessidade − acumulado, mínimo 0 — os dois são do app, não da
planilha.

Valores de teste: necessidade 10000, investimento 2000, aportes 500 (dez/25),
1000 + juros 50 (jan/26), 1000 − juros 20 (fev/26) → acumulado 4530,00,
percentual 0,453.

---

## Defeitos conhecidos da planilha

Replicar a **intenção**, não o defeito. Cada item abaixo deve ser corrigido na
implementação — e conferido com o usuário se a correção mudar números que ele
já usa.

1. **Rateio de custos com denominador truncado.** `F22 = SUM(F12:F16)` soma
   apenas 5 linhas, mas o total `J22 = SUM(J12:J21)` soma 10. Com mais de 5
   operações no mês, os percentuais não fecham 100% e o custo rateado sai
   errado. **Correção**: denominador sobre todas as operações do período.

2. **Custo esquecido na desmontagem do JBSS3.** `M40 = SUM(M37:M39)`, sem
   subtrair o custo, enquanto todas as outras estruturas fazem `SUM(...) −
   custo`. Aquele resultado está inflado. **Correção**: sempre subtrair custo.

3. **Divisão sem guarda em evoluções.** `Evolução das Receitas` (F25) e
   `Evolução dos Gastos` (F152) dividem pelo mês anterior sem proteção. Mês
   anterior zerado = erro. **Correção**: aplicar a mesma guarda usada no resto
   da planilha (retornar 0).

4. **Independenciômetro sem guarda.** `C201 = D23/D151` quebra se a despesa do
   mês for zero. **Correção**: guarda retornando 0.

---

## Não mapeado ainda

Estas regiões **não foram extraídas**. Silêncio aqui significa "não verificado",
não "não existe". Ao implementar qualquer uma delas, leia a planilha primeiro e
atualize este documento.

- Seção **META 1..10** dentro do CONTROLE FINANCEIRO PESSOAL (linhas 203-214):
  só valores digitados por mês e `TOTAL = SUM`. **Presumido** que sejam os
  aportes destinados a cada meta (o mesmo dado da coluna APORTE da aba
  PLANEJAMENTO e METAS); confirmar com o usuário antes de criar algo separado.
- Aba **CAPA**
- Detalhamento das abas RENDA FIXA / FII / CRIPTOMOEDAS / IMOBILIZADO abaixo
  das linhas de total (padrão presumido idêntico ao de AÇÕES, **não
  confirmado**)
