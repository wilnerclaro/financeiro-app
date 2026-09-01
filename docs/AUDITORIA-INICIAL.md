# Auditoria inicial

Levantamento do backend em relação à planilha de origem e a boas práticas.
Base: `develop` / `feature/dashboard-resumo-mensal`.

Ordem sugerida de ataque: **1 → 2 → 3 → 4**. Os demais são dívida a resolver
quando o módulo correspondente for tocado.

---

## 1. CRÍTICO — A API está aberta

`SecurityConfig` termina em `.anyRequest().permitAll()`, e o `usuarioId` chega
por `@RequestParam` ou no corpo da requisição. Qualquer chamador informa o UUID
de qualquer usuário e lê ou altera os dados dele.

`Usuario` tem `senhaHash`, mas não existe fluxo de autenticação.

Num produto multiusuário com dado patrimonial, isso é o item de maior
gravidade.

**Direção decidida**: JWT stateless. A configuração já aponta para lá
(`SessionCreationPolicy.STATELESS`, `csrf` desabilitado,
`spring-boot-starter-security` presente), e clientes Angular + mobile tornam
cookie de sessão inconveniente.

**Escopo real do refactor**: o `usuarioId` precisa sair de parâmetro/corpo e
passar a vir do principal do token. Isso toca todos os controllers, DTOs de
request e assinaturas de service existentes. Enquanto o cliente disser quem
ele é, a API continua vulnerável mesmo com token válido.

**Fica em aberto para decisão**: emissão e rotação de token, refresh, e o que
fazer com `.env` — a senha do banco circulou fora do repositório e convém
trocar.

---

## 2. CRÍTICO — Zero testes

Existe apenas `FinanceiroApiApplicationTests` (context load). O `pom.xml` já
declara cinco starters de teste, todos sem uso.

Com mutação de saldo em produção e cálculo financeiro no dashboard, é o
segundo maior risco. É também o que impede que os itens desta auditoria virem
dívida permanente.

Prioridade de cobertura: mutação de saldo → dashboard → isolamento entre
usuários.

---

## 3. ALTO — `orcamentos_mensais` existe no banco e não no código

A migration `V6` criou a tabela com `usuario_id`, `categoria_id`, `ano`, `mes`,
`tipo`, `valor_previsto` e unicidade por (usuário, categoria, ano, mês). Não há
entity, repository, service nem controller. Branch `feature/orcamento-mensal`
em aberto.

Consequência funcional: o `DashboardFinanceiroService` só agrega
`status = 'PAGO'`, ou seja, apenas o **REALIZADO**. O **PREVISTO** da planilha
não existe na aplicação, e é metade do conceito — a planilha trata os dois como
par em receitas, despesas e orçamento.

Destravar isso é o caminho mais curto para paridade com a planilha.

---

## 4. ALTO — `saldo_atual` sem controle de concorrência

`Conta.saldoAtual` é campo mutável, atualizado por
`LancamentoFinanceiroService.movimentarSaldo`/`estornarSaldo`, sem `@Version`.
Duas requisições concorrentes produzem lost update e o saldo diverge em
silêncio.

A existência de `CorrigirSaldoInicialContaRequest`, que recalcula a partir de
`calcularSaldoMovimentadoPorConta`, sugere que a divergência já foi observada.

**Decisão em aberto** (arquitetural, não trivial):
- manter materializado e adicionar `@Version` + tratamento de conflito, ou
- derivar o saldo dos lançamentos e materializar só como cache explícito

A lógica de `atualizar()` em si está correta: estorna com a conta antiga antes
de trocar, depois removimenta com a nova.

---

## 5. MÉDIO — `ATRASADO` nunca acontece após a escrita

`definirStatusInicial` compara `dataVencimento` com `LocalDate.now()` apenas na
criação e na atualização. Um lançamento `PENDENTE` que vence amanhã permanece
`PENDENTE` indefinidamente — não há job de transição nem cálculo derivado na
leitura.

Duas saídas: agendamento que promove os vencidos, ou status derivado em
consulta. A segunda evita estado inconsistente no banco.

---

## 6. MÉDIO — Falta a dimensão ativa/passiva nas receitas

`TipoCategoria` tem apenas `RECEITA` e `DESPESA`. A planilha separa receita em
**FONTE ATIVA** e **FONTE PASSIVA**, e o **Independenciômetro** — a métrica
central do controle financeiro — é `receita passiva / despesa total`.

Sem essa dimensão, a métrica principal da planilha não é calculável.

---

## 7. Qualidade de código

Pontos concretos, verificáveis:

- **`Boolean` tratado de duas formas.** `CategoriaService` faz
  `if (!categoriaPai.getAtiva())` — unboxing de `Boolean`, NPE se nulo.
  `ContaService` e `LancamentoFinanceiroService` usam
  `Boolean.TRUE.equals(...)`. Padronizar (ou migrar para `boolean` primitivo,
  já que as colunas são `NOT NULL`).

- **`buscarUsuario` duplicado em quatro services.** Mesmo método privado
  copiado em `Categoria`, `Conta`, `LancamentoFinanceiro` e, em variante,
  `Dashboard`.

- **Enums comparados por string.**
  `categoria.getTipo().name().equals(tipoLancamento.name())` funciona por
  coincidência de nomenclatura entre dois enums distintos. Renomear uma
  constante quebra em silêncio, sem erro de compilação.

- **`@ExceptionHandler(Exception.class)` engole o erro.** Retorna 500 genérico
  e **não loga nada** — não existe um único logger no projeto. Em produção,
  falha inesperada não deixa rastro.

- **`LancamentoFinanceiroService` com ~14 KB** misturando CRUD, mutação de
  saldo e construção de `Specification` inline. O builder de filtros é
  candidato natural a classe própria.

- **Modelo anêmico.** Entidades com `@Setter` em tudo e regra de negócio nos
  services. Mover `movimentarSaldo`/`estornarSaldo` para dentro de `Conta`
  seria o encaminhamento clássico — e conversa diretamente com o item 4.

- **Repository misturando dialetos.** `DashboardFinanceiroRepository` tem
  `buscarResumoMensal` em SQL nativo e `buscarResumoPorCategoria` em JPQL.

- **Finais de linha inconsistentes.** Vários arquivos em CRLF, outros em LF, e
  `CategoriaController` tem os dois no mesmo arquivo. O Spotless normaliza —
  faça isso em commit isolado (`chore: aplica formatacao`) antes do trabalho
  dos agentes, para não misturar ruído com mudança real.

---

## O que está bom

Registrado para não ser "corrigido" por engano:

- Separação por feature, consistente em todos os módulos
- Interfaces `*ApiDoc` isolando anotações Swagger dos controllers — padrão
  incomum e acertado
- `open-in-view: false` e `ddl-auto: validate`
- Migrations versionadas com constraints nomeadas e trigger de `atualizado_em`
- `BigDecimal` com `precision`/`scale` explícitos em todo campo monetário
- `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` nas entidades JPA
- Soft delete coerente em todo o domínio
- Percentual do dashboard com `RoundingMode.HALF_UP` e guarda de divisão por
  zero — exatamente como a planilha faz
