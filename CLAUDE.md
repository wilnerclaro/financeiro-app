# financeiro-app

Sistema de gestão financeira e patrimonial pessoal, migrado de uma planilha
Excel de controle (`M4A2_PlanilhaDoMira`). A planilha é a fonte de requisitos;
o glossário e as regras de cálculo derivados dela estão na skill
`glossario-financeiro`.

> Este arquivo é carregado no contexto de **todos** os subagentes. Mantenha-o
> curto. Detalhe de domínio vai para a skill; detalhe de papel vai para o
> arquivo do agente em `.claude/agents/`.

## Stack

- Java 21, Spring Boot 4.0.6, Maven
- PostgreSQL 16 (Docker Compose), Flyway (`ddl-auto: validate`)
- JPA/Hibernate, `open-in-view: false`
- Lombok + MapStruct, springdoc-openapi
- Frontend: Angular (ainda não iniciado)

## Estado atual

Implementados: `usuario`, `categoria`, `conta`, `lancamentofinanceiro`,
`dashboardfinanceiro`.

Não implementados: orçamento (a tabela `orcamentos_mensais` existe na migration
V6 **sem código Java**), e todo o lado de investimentos (carteira, trade,
opções, metas, custo de operações).

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
tratados no `GlobalExceptionHandler` com envelope `ApiErrorResponse`.
Mensagens em português **sem acento** ("Nao e possivel...") — convenção
existente, mantenha.

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

**Regra inegociável**: os quatro módulos legados estão na lista de exclusão do
JaCoCo por serem anteriores ao gate. **Nenhum módulo novo entra nessa lista.**
Código novo nasce coberto. Módulo legado sai da lista conforme for coberto.

Não fatie método coeso só para reduzir contagem de linhas. Legibilidade vence
métrica.

## Isolamento por usuário

O produto é **multiusuário**. Todo dado é privado do seu usuário.

Hoje o `usuarioId` chega por parâmetro ou corpo da requisição, o que é uma
falha de segurança em aberto (ver auditoria). Após o refactor de
autenticação, ele vem **exclusivamente do principal do token** — nunca do
cliente.

Toda query filtra por usuário. Todo módulo novo tem teste que prova que o
usuário A não alcança dado do usuário B.

## Ao trabalhar aqui

1. Leia a auditoria antes de mudança estrutural.
2. Regra financeira vem da planilha, não de intuição. Se não estiver na skill
   `glossario-financeiro`, **pare e sinalize** em vez de inventar.
3. Se a planilha e o código divergirem, isso é um achado a reportar, não uma
   ambiguidade a resolver sozinho.
