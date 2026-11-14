# Protótipo do front: PrimeNG 22

Guia de referência do front do financeiro e checklist para conferir se o front implementado bate com o que foi proposto. Se o protótipo navegável e este documento divergirem, vale o protótipo. O Figma guarda capturas do protótipo, então ele também cede ao protótipo.

Atualizado em 14/11/2026.

## Onde está cada coisa

| O quê | Onde | Para que serve |
| --- | --- | --- |
| App navegável (PrimeNG 22) | [Financeiro PrimeNG](https://claude.ai/artifact/Uzjr55tBvJmxsHDcAMUSMj) | Clicar e testar estados e animações nos dois temas |
| Figma: telas do app | [Página "App · PrimeNG 22"](https://www.figma.com/design/kDe6zUxKPvxKQO7GGRB6CG?node-id=17-2) | Cada tela no escuro, no claro e no celular, lado a lado |
| Figma: desenho original | [Página "Telas"](https://www.figma.com/design/kDe6zUxKPvxKQO7GGRB6CG) | Protótipo clicável do desenho, incluindo as telas do Android |
| Figma: comparação | [Página "PrimeNG x Material"](https://www.figma.com/design/kDe6zUxKPvxKQO7GGRB6CG?node-id=12-2) | Histórico da escolha da biblioteca |
| Documento vivo (com comentários) | [Claude Doc](https://claude.ai/artifact/8TGvx3kZqV3j5dznQu8zRC) | Mesma informação deste arquivo, editável |

Os links são privados: o dono compartilha pelo menu Share de cada um.

## Base técnica e tema

O front usa PrimeNG 22 sobre Angular 22. A decisão foi tomada em 14/11/2026, depois de comparar com Angular Material, Optimus UI e Spartan UI.

| Item | Valor no protótipo |
| --- | --- |
| Angular | 22.2, componentes standalone, signals |
| PrimeNG | 22.1.5, com `@primeuix/themes` 3 e `primeicons` |
| Gráficos | chart.js, via `p-chart` |
| Tema | Preset Aura com primário esmeralda (`definePreset`), ripple ligado |
| Modo escuro | Classe `app-dark` no `html`; o escuro é o padrão e o botão sol/lua troca |
| Rotas | `withHashLocation` e `withViewTransitions` |
| Fonte | Inter |
| Avisos | `p-toast` no topo central, um `MessageService` para o app todo |
| Licença | PrimeUI Community, grátis para uso individual; vence em 14/11/2027 |

A chave entra em `providePrimeNG({ license: environment.primeuiLicense, ... })` e fica em `src/environments/environment.ts`. A verificação é feita no navegador, sem internet. Sem chave, ou com ela vencida, aparece o selo vermelho "Invalid PrimeUI License" no canto inferior direito.

Atenção no PrimeNG 22: `p-card`, `p-message`, `p-chip`, `p-table` e `p-treetable` não têm mais `styleClass`; use `class`. O `p-button` ainda tem. O toggler da treetable é `p-treetable-toggler`.

### Cores de dinheiro

Receita é sempre azul e despesa é sempre vermelha, em qualquer tela.

| Token | Uso | Claro | Escuro |
| --- | --- | --- | --- |
| `--rec` | Receita, entradas, sinal + | `#1F5FBF` | `#7CB4FF` |
| `--des` | Despesa, saídas, sinal −, passou do previsto | `#C62828` | `#FF6B6B` |
| `--ok` | Sobra, folga, abaixo do previsto, até 70% de uso | `#0B7A5A` | `#5FD3A6` |
| `--amb` | Uso de 70% a 89% | `#C48A00` | `#F5C84B` |
| `--lar` | Uso de 90% a 100% | `#D9620B` | `#FF9248` |
| `--fundo` | Fundo da página | `#F4F5F1` | `#0B0F0E` |

### Escala do orçamento

A mesma função `nivel(pct)` colore anéis, barras, células da tabela do ano e textos de desvio.

| Uso do previsto | Cor | Token |
| --- | --- | --- |
| Abaixo de 70% | Verde | `--ok` |
| De 70% a 89% | Amarelo | `--amb` |
| De 90% a 100% | Laranja | `--lar` |
| Acima de 100% | Vermelho | `--des` |

O Independenciômetro usa outra escala: o verde fica mais escuro conforme a renda passiva cobre mais das despesas, e mais claro quando ela recua.

## Estrutura do app

| Rota | Tela | Como se chega |
| --- | --- | --- |
| `/` | Início | Menu, marca do app |
| `/lancamentos` | Lançamentos | Menu; "Ver todos" no Início |
| `/lancar` | Lançar | Botão "+ Lançar" no topo |
| `/orcamento` | Orçamento do mês | Menu; link no quadro "Saiu por categoria" |
| `/orcamento/ano` | Orçamento do ano | Seletor Mês/Ano; "Ver o ano todo" no Início |
| `/orcamento/previsto` | Editar previsto | Botão "Editar previsto" no Orçamento |
| `/categorias` | Categorias | Menu |
| `/contas` | Contas | Menu |
| `/importar` | Importar extrato, Revisar e Pronto | Botão "Importar extrato" no topo |

### Cabeçalho

- Fixo no topo, com fundo translúcido e desfoque.
- Marca "financeiro" com o logo; leva ao Início.
- Menu em pílulas: Início, Lançamentos, Orçamento, Categorias, Contas. O item ativo fica preenchido com a cor primária; Orçamento continua ativo nas telas do ano e do previsto.
- À direita: Importar extrato (contorno), botão de tema sol/lua com dica e "+ Lançar" (cheio).
- Abaixo de 1100 px some o Importar extrato. Abaixo de 700 px o menu vira uma faixa rolável numa segunda linha.

### Padrões comuns

- Cartões com cantos de 22 px. Entram com um leve deslizar para cima, em cascata, de 30 a 60 ms entre um e outro. Sem animação quando o sistema pede movimento reduzido.
- Grade de 12 colunas que vira uma coluna abaixo de 900 px.
- Rótulos de seção em maiúsculas pequenas e cinza.
- Avatar quadrado com a inicial: azul para receita, vermelho para despesa, amarelo para conta a vencer, verde para conta ou status bom, cinza para inativo ou cancelado.
- Valores com algarismos de largura fixa, sinal + para receita e − para despesa, sempre em reais com centavos.
- Números de destaque sobem ao aparecer (contador animado).
- Cada ação que grava mostra um aviso dizendo o que aconteceu, por exemplo: "Lançado: Padaria · R$ 45,90. Saldo do Nubank: R$ 4.812,37."
- Menus de ações abrem pelo botão ⋯ (`p-menu` em popup).

## Telas

### Início · [Figma](https://www.figma.com/design/kDe6zUxKPvxKQO7GGRB6CG?node-id=17-9)

| Bloco | Conteúdo | Componentes |
| --- | --- | --- |
| Saudação | Data e dias para fechar o mês; "Você já guardou 36% do que entrou", com o número em verde | Texto |
| Aviso de revisão | 5 itens para revisar, com a origem e o botão "Revisar agora" | `p-message`, `p-button` |
| Sobrou em novembro | Sobra com contador; Entrou em azul e Saiu em vermelho com a variação contra outubro; barras de jun a nov | `p-card`, `p-tag`, `p-chart` bar |
| Independenciômetro | Anel com o % de despesas pagas pela renda passiva, selo "exemplo", frase "1 de cada 5 reais", variação em pontos, quanto falta para 25% | `p-knob`, `p-tag` |
| Lançar agora | Valor, três categorias rápidas e "Lançar · Nubank · hoje" | `p-inputnumber`, `p-selectbutton`, `p-button` |
| Para pagar | Contas a vencer com total; vencida em vermelho com botão cheio, a vencer com contorno | `p-avatar`, `p-button` |
| Orçamento | Três anéis (Alimentação, Moradia, Lazer) e "98,8% usado" em laranja | `p-knob` |
| Últimos lançamentos | Quatro últimos com categoria, conta e data; "Ver todos" | lista, `p-avatar` |
| Para onde foi o dinheiro | Fatias por grupo | `p-metergroup` |

Regras: a barra do mês fica verde quando a sobra é 30% ou mais do que entrou, e vermelha abaixo disso. "Lançar agora" grava e mostra o aviso com o novo saldo.

### Lançamentos · [Figma](https://www.figma.com/design/kDe6zUxKPvxKQO7GGRB6CG?node-id=17-13)

- Cabeçalho com entradas e saídas do mês e navegação de mês (‹ nov/2026 ›).
- Busca por descrição, categoria ou valor (`p-iconfield` + `p-inputicon`), filtrando na hora.
- Filtros Todos, A pagar (contador vermelho), Receitas, Despesas (`p-selectbutton` + `p-badge`) e "Mais filtros".
- Um cartão por dia com o saldo do dia (azul se positivo, vermelho se negativo).
- Cada linha: avatar, descrição, categoria › subcategoria · conta, valor com sinal e ⋯ com Editar, Duplicar, Mudar categoria e Cancelar lançamento.
- Cancelado: texto riscado, avatar cinza, selo "Cancelado", fora do saldo do dia.
- Lateral: "A pagar" com borda vermelha e botão Pagar; "Saiu por categoria" com os cinco grupos mais usados, barra na cor da escala e link para o Orçamento.
- No celular somem os avatares e o selo de cancelado.

### Lançar · [Figma](https://www.figma.com/design/kDe6zUxKPvxKQO7GGRB6CG?node-id=17-17)

- Tipo em `p-selectbutton` com ícone: Despesa, Receita, Transferência.
- Valor grande no centro (`p-inputnumber` BRL): vermelho em despesa, azul em receita, neutro em transferência. Descrição abaixo.
- Categoria: grade com as sete mais usadas; a escolhida fica com borda primária. Em transferência a grade dá lugar a "Sai de" e "Entra em" (`p-select`) e ao aviso de que transferência não conta como receita nem despesa.
- Conta (`p-select`) e Quando (`p-datepicker`), com rótulo flutuante (`p-floatlabel`).
- "Já paguei" (`p-toggleswitch`): tira o valor do saldo agora; desligado agenda com vencimento.
- "Mais detalhes" em `p-accordion`: competência e observação.
- "Lançar R$ X" (cheio) e "Lançar e fazer outro" (contorno).

### Orçamento do mês · [Figma](https://www.figma.com/design/kDe6zUxKPvxKQO7GGRB6CG?node-id=17-21)

- Topo: seletor Mês/Ano, navegação de mês, "Copiar de outubro" e "Editar previsto".
- "Copiar de outubro" pede confirmação (`p-confirmdialog`) e informa quantas categorias foram copiadas e quantas mantidas.
- Orçamento usado: anel com 98,8% em laranja, gasto com contador, previsto total, quanto ficou abaixo e a legenda da escala.
- Atenção: grupos acima do previsto com alerta e "Ver lançamentos"; grupos com folga em verde.
- Despesas por grupo e subitem (`p-treetable`): Categoria, Previsto, Realizado, Desvio (▲ vermelho / ▼ verde), Uso (barra + % na cor da escala). "Abrir tudo / Recolher tudo". Rodapé com o total.
- Receitas em cartões: nome, fonte ativa ou passiva, valor em azul e previsto, ou ▲ quanto passou.

### Editar previsto · [Figma](https://www.figma.com/design/kDe6zUxKPvxKQO7GGRB6CG?node-id=17-25)

- Link de volta ao Orçamento; categoria com a média dos últimos 6 meses.
- Previsto por mês em campo grande (`p-inputnumber` BRL).
- "Vale para" (`p-selectbutton`): Só novembro, Próximos meses (contador de 2 a 24), Até dezembro, 12 meses, Escolher meses.
- Grade dos 12 meses seguintes; tocar num mês inclui ou tira e muda o modo para "Escolher meses". Mês fechado não aparece.
- Meses que já têm outro previsto ficam tracejados em amarelo, com `p-message` e a escolha "Manter o que já tem" ou "Trocar".
- O botão diz quantos meses vão mudar ("Salvar em 10 meses"); mantidos não contam; sem meses fica desligado. Salvar avisa e volta ao Orçamento.
- Lateral: "Repetir em outras categorias" e "Ou copie um mês inteiro".
- API: `POST /api/orcamentos/repetir` (`categoriaId`, `valorPrevisto`, `meses`, `substituirExistentes`, até 24 meses) e `POST /api/orcamentos/{ano}/{mes}/copiar` (`meses`, `substituirExistentes`). A resposta traz, por mês, gravados e mantidos.

### Orçamento do ano · [Figma](https://www.figma.com/design/kDe6zUxKPvxKQO7GGRB6CG?node-id=17-29)

- "2026 até agora" e seletor Mês/Ano.
- Quatro números com contador: Entrou, Saiu, Guardou (com % e meta de 30%) e Independenciômetro do ano.
- Dois `p-chart` de linha: realizado com área e previsto tracejado.
- `p-table` com a primeira coluna fixa: jan a dez, Total e Média; mês atual destacado.
- Receitas em fonte ativa, passiva e total; despesas por grupo, total e "Evolução dos gastos" (▲/▼ % contra o mês anterior).
- Célula acima do previsto em vermelho com fundo; de 90% a 100% em laranja; sem previsto e futuros em cinza; a dica mostra o % do previsto.

### Categorias · [Figma](https://www.figma.com/design/kDe6zUxKPvxKQO7GGRB6CG?node-id=17-32)

- Abas "Despesas · 10" e "Receitas · 2".
- Cartão por grupo: inicial, nome, gasto do mês e ⋯ (Renomear, Nova subcategoria, Ver lançamentos, Inativar).
- Subcategorias em `p-chip`; inativa riscada; excesso vira "+ N"; botão tracejado "+ Sub" preenche o formulário com o grupo.
- Grupos de receita mostram a fonte num selo.
- Lateral fixa "Nova categoria": Nome, Tipo, Dentro de (`p-select` com busca; "Nenhum" cria grupo), regra de dois níveis, "Criar categoria" desligado sem nome.

### Contas · [Figma](https://www.figma.com/design/kDe6zUxKPvxKQO7GGRB6CG?node-id=17-35)

- Patrimônio das contas ativas com contador; "Nova conta" abre `p-dialog` (Nome, Tipo, Saldo de hoje).
- Composição em `p-metergroup`: Investimento, Poupança, Disponível.
- Cartão por conta: saldo e variação desde o saldo inicial (▲ verde, ▼ vermelho) ou "Sem movimento". Inativa tracejada com "Reativar".
- ⋯: Ver lançamentos, Renomear, Corrigir saldo inicial, Inativar.
- "Corrigir saldo inicial" abre `p-dialog` ([Figma](https://www.figma.com/design/kDe6zUxKPvxKQO7GGRB6CG?node-id=17-37)) com o novo saldo inicial e a prévia do saldo atual recalculado.

### Importar extrato · [Enviar](https://www.figma.com/design/kDe6zUxKPvxKQO7GGRB6CG?node-id=17-39) · [Revisar](https://www.figma.com/design/kDe6zUxKPvxKQO7GGRB6CG?node-id=17-42) · [Pronto](https://www.figma.com/design/kDe6zUxKPvxKQO7GGRB6CG?node-id=17-44)

Três passos num `p-stepper` linear. Nada entra no caixa antes da revisão.

1. Enviar: área de arrastar ou clicar, arquivo lido (transações, período, banco detectado), "Trocar", conta de destino e "Ler extrato e revisar" com carregando. Lateral: como baixar do banco, formatos (OFX agora; CSV e PDF depois), importações anteriores.
2. Revisar: filtros A revisar, Novas, Conciliadas. Por item: `p-checkbox`, descrição e texto do banco, selos de situação e origem, nota de junção ou conciliação, categoria sugerida (`p-select` com busca) com o motivo, valor. Sem categoria o campo fica vermelho e "Confirmar" desligado. Sanfona com os já lançados. Rodapé com totais.
3. Pronto: sucesso, quantos entraram, totais, novo saldo, regra criada para a próxima importação, "Ver lançamentos" e "Importar outro".

A conta conciliada com o previsto não soma nas saídas: só marca o previsto como pago.

## Checklist de conferência

### Geral

- [ ] Angular 22 e PrimeNG 22 com tema Aura em verde esmeralda
- [ ] Chave Community configurada; nenhum selo "Invalid PrimeUI License"
- [ ] Tema escuro padrão e sol/lua trocando tudo, inclusive gráficos
- [ ] Receita sempre azul e despesa sempre vermelha, nos dois temas
- [ ] Escala do orçamento igual em todas as telas
- [ ] Menu com os cinco destinos e item ativo destacado
- [ ] Toda ação que grava mostra um aviso
- [ ] Animação de entrada em cascata, respeitando movimento reduzido
- [ ] Nenhuma tela rola para o lado no celular (390 px)

### Por tela

- [ ] Início: nove blocos; regra dos 30% nas barras; "Lançar agora" grava
- [ ] Lançamentos: busca e filtros; cartão por dia com saldo; cancelado riscado e fora do saldo; menu ⋯
- [ ] Lançar: cor do valor por tipo; transferência com Sai de / Entra em; Já paguei; Mais detalhes
- [ ] Orçamento do mês: anel do total; Atenção; subitens abrindo; Copiar com confirmação
- [ ] Editar previsto: modos de "Vale para"; grade de meses; Manter/Trocar; nº certo de meses no botão
- [ ] Orçamento do ano: quatro números; dois gráficos; coluna fixa; cores das células; Evolução dos gastos
- [ ] Categorias: abas; chips com inativa e "+ N"; "+ Sub" preenche o formulário; Criar desligado sem nome
- [ ] Contas: patrimônio; composição; variação; inativa; diálogo de saldo inicial com prévia
- [ ] Importar: três passos; Confirmar desligado sem categoria; conciliada fora das saídas; Pronto com a regra

### Dados reais

- [ ] Os números vêm da API
- [ ] Editar previsto usa `POST /api/orcamentos/repetir` e mostra gravados e mantidos
- [ ] Copiar de outubro usa `POST /api/orcamentos/{ano}/{mes}/copiar`

## Diferenças em relação ao desenho original

| Ponto | Desenho original | Protótipo PrimeNG 22 | Situação |
| --- | --- | --- | --- |
| Busca global | "Buscar ou lançar" com Ctrl K no topo | Não existe; busca só em Lançamentos | Falta fazer, se ainda for desejada |
| Transferência | Tela própria | Terceiro tipo dentro de Lançar | Mantido: um formulário só |
| Corrigir saldo inicial | Quadro fixo em Contas | Diálogo pelo menu ⋯ | Mantido |
| "Dentro de" (Categorias) | Botões com os grupos | Lista com busca | Mantido: cabem os 12 grupos |
| Aviso de endpoint no Orçamento | Faixa sobre a API que faltava | Removido | A API de previsto já existe |
| Orçamento por subitem | Tabela simples | Grupos que abrem nos subitens | Melhorado |
| Importar | Duas telas | Uma tela com três passos e Pronto | Melhorado |
| Telas do Android | Início, Lançar e A revisar | Não feitas no Angular | Ficam para o app Android |

## Fora do protótipo

- Telas próprias do Android: só no desenho original (página "Telas" do Figma).
- Investimentos e Metas: fora, o foco é o controle financeiro.
- Importação por CSV e PDF de fatura: futuras, sem fluxo desenhado.
- Login e configurações: não fazem parte do protótipo.
- Renovar a licença PrimeUI Community antes de 14/11/2027.
