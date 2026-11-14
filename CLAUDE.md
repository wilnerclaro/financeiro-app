# financeiro-app

Sistema de gestão financeira e patrimonial pessoal, migrado de uma planilha
Excel de controle (`M4A2_PlanilhaDoMira`). A planilha é a fonte de requisitos;
o glossário e as regras de cálculo derivados dela estão na skill
`glossario-financeiro`.

> Este arquivo é carregado no contexto de **todos** os subagentes. Mantenha-o
> curto. Detalhe de domínio vai para a skill; detalhe de papel vai para o
> arquivo do agente em `.claude/agents/`.

## Stack

- Java 25, Spring Boot 4.1.2, Maven
- PostgreSQL 16 (Docker Compose), Flyway (`ddl-auto: validate`)
- Testes de integração com Testcontainers (exigem Docker rodando)
- JPA/Hibernate, `open-in-view: false`
- Lombok + MapStruct, springdoc-openapi
- Frontend: Angular (ainda não iniciado)

## Estado atual

Implementados: `autenticacao` (JWT), `usuario`, `categoria` (com receita
ativa/passiva e categorias padrão da planilha no cadastro), `conta` (saldo
guardado na conta com `@Version`; cartão de crédito com fechamento e
vencimento), `lancamentofinanceiro` (ATRASADO calculado na leitura, fatura do
cartão), `transferencia`, `orcamento` (Previsto x Realizado e resumo anual),
`meta` (metas e sonhos) e `dashboardfinanceiro` (resumo mensal e tela Início
com o Independenciômetro).

Não implementados: todo o lado de investimentos (carteira, trade, opções,
custo de operações).

Regras que valem para o código novo:
- `ATRASADO` não é gravado: no banco só há `PENDENTE`, `PAGO` e `CANCELADO`;
  a resposta e o filtro derivam o atraso de `dataVencimento < hoje` (Clock).
- Compra no cartão: `dataCompetencia` do pedido é a data da compra
  (`dataCompra`); vencimento e competência passam a ser o vencimento da fatura.
- Transferência não é receita nem despesa e não entra no Previsto x Realizado.
- Percentual é fração com 4 casas via `common.util.Fracao` (guarda → 0).
- Dinheiro calculado arredonda com `common.util.Dinheiro` (2 casas, HALF_UP);
  ano e mês da API passam por `common.util.Competencia` (2000 a 2100).
- Enum vai no DTO como enum, nunca como `String` com `name()`.
- Dado de referência (como as categorias padrão do cadastro) mora no banco,
  semeado por migration (`categorias_padrao`, V14), nunca em constante Java.
- Categoria com lançamento ou previsto não muda de tipo; conta com lançamento
  não vira cartão nem deixa de ser cartão.
- Saldo só muda por `ContaService.creditar/debitar`, que travam a linha da conta
  (`SELECT ... FOR UPDATE`) antes de mexer; transferência trava as duas contas
  em ordem de id. Gravações simultâneas entram em fila, sem deadlock nem 500.
- Service só usa o repository do próprio módulo; dado de outro módulo vem do
  service dele (regra no `ArquiteturaTest`).

Achados críticos da auditoria inicial estão em `docs/AUDITORIA-INICIAL.md`.
**Leia antes de propor mudança estrutural.**

## Convenções obrigatórias

Estas convenções já existem no código. Imite-as; não invente alternativas.

**Referências canônicas** — ao criar algo novo, espelhe estes arquivos:
- Módulo completo: `categoria/`
- Controller: `categoria/controller/CategoriaController.java`
- Interface de documentação: `categoria/docs/CategoriaApiDoc.java`
- Service: `conta/service/ContaService.java`
- Migration: `db/migration/V5__criar_tabela_lancamentos_financeiros.sql`

**Comentários (pedido do Wilner)**: siga o Clean Code. Não escreva Javadoc
nem comentário que repete o nome da classe, do método ou do campo, nem
separador, nem comentário de célula da planilha ou de fase. O nome deve
explicar o código. A regra de negócio e a origem na planilha ficam no
glossário; a descrição para o front-end fica no `@Schema`/`@Operation`.
Migrations também vão sem comentário.

**Estrutura de módulo**: pacote por feature, com
`controller/ docs/ dto/ entity/ enums/ mapper/ repository/ service/`.

**Controller**: implementa a interface `<Nome>ApiDoc`. Todas as anotações
Swagger vivem na interface; o controller só orquestra. Retorna
`ResponseEntity`; `201` com `Location` na criação, `204` na inativação.

**Service**: `@Service @RequiredArgsConstructor`, `@Transactional` na escrita e
`@Transactional(readOnly = true)` na leitura. Métodos públicos em português:
`criar`, `listar`, `buscarPorId`, `atualizar`, `inativar`, `ativar`. Helpers
privados: `buscarXDoUsuario`, `validarY`.

**Soft delete**: nunca `DELETE` físico. Campo booleano `ativa`/`ativo`;
`DELETE /{id}` inativa, `PATCH /{id}/ativar` reativa.

**DTOs**: records. `Criar<X>Request`, `Atualizar<X>Request`, `<X>Response`.
Entidade **nunca** cruza a fronteira do controller.

**Erros**: `BusinessException` → 400, `ResourceNotFoundException` → 404,
conflito de banco (`DataIntegrityViolationException`) → 409, JSON ou parâmetro
inválido → 400, tudo tratado no `GlobalExceptionHandler` com envelope
`ApiErrorResponse`. Erro 500 é sempre logado com stack trace.
Mensagens em português **com acento** ("Não é possível...") — elas chegam à
tela do usuário.

**Contrato da API**:
- `PUT` não altera `ativa`; status só muda por `DELETE` (inativa) e
  `PATCH /{id}/ativar` (reativa), ambos idempotentes.
- Nomes são guardados como digitados (só espaços normalizados); a checagem de
  duplicidade ignora maiúsculas.
- Listagens devolvem `{ "content": [...], "page": { size, number,
  totalElements, totalPages } }` (`WebConfig`, `VIA_DTO`).
- Percentual é fração de 0 a 1 com 4 casas (`0.2500` = 25%); o front formata.
- Todo endpoint (fora de `/api/auth`) exige o header
  `Authorization: Bearer <accessToken>`.
- "Hoje" vem do bean `Clock` (fuso `America/Sao_Paulo`); nunca use
  `LocalDate.now()` sem o `clock`.

**Migrations**: `V<n>__nome_em_portugues.sql`. Prefixos `fk_`, `uk_`, `ck_`,
`idx_`. Trigger `definir_atualizado_em` em toda tabela. Nunca edite uma
migration já aplicada; crie a próxima.

**Dinheiro**: sempre `BigDecimal` / `NUMERIC`. Nunca `double` ou `float`.
Escala monetária padrão: `NUMERIC(19,2)`. Quantidade de cripto e cotação
exigem escala maior — ver a skill `glossario-financeiro`.

**Git**: conventional commits com descrição em português (`feat:`, `fix:`,
`chore:`, `test:`, `docs:`). Branch `feature/<nome-em-portugues>` → PR →
`develop`.

## Qualidade

O build é a autoridade, não a opinião. Todo trabalho termina com:

```
mvn verify
```

Isso roda Spotless (formatação), Checkstyle (estilo), JaCoCo (cobertura) e
ArchUnit (regras de arquitetura como teste). Ver `docs/QUALIDADE-BUILD.md`.

**Regra inegociável**: a exclusão do JaCoCo só tem o que não tem lógica (DTO,
entidade, enum, docs, config, mappers gerados). **Nenhum service ou controller
entra nela.** Código novo nasce coberto.

Não fatie método coeso só para reduzir contagem de linhas. Legibilidade vence
métrica.

## Isolamento por usuário

O produto é **multiusuário**. Todo dado é privado do seu usuário.

O usuário vem **exclusivamente do token JWT**, nunca do cliente. No controller,
receba-o com `@UsuarioLogado UUID usuarioId` (pacote `security`) e repasse ao
service; DTO de request nunca tem `usuarioId`. Só `/api/auth/cadastro`,
`/login`, `/refresh`, Swagger e health são públicos; o resto exige
`Authorization: Bearer <accessToken>` e responde 401 sem ele.

Tokens: acesso de 15 min e refresh de 7 dias (claim `tipo` separa os dois),
HMAC-SHA256 com `JWT_SECRET` do `.env`. Senha em BCrypt. E-mail é gravado em
minúsculas e é único sem diferenciar maiúsculas.

Toda query filtra por usuário. Todo módulo novo tem teste que prova que o
usuário A não alcança dado do usuário B.

## Ao trabalhar aqui

1. Leia a auditoria antes de mudança estrutural.
2. Regra financeira vem da planilha, não de intuição. Se não estiver na skill
   `glossario-financeiro`, **pare e sinalize** em vez de inventar.
3. Se a planilha e o código divergirem, isso é um achado a reportar, não uma
   ambiguidade a resolver sozinho.
