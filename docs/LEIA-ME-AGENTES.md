# Pacote de agentes — como instalar

Descompacte na pasta que **contém** o `financeiro-app`, para que os arquivos
caiam por cima do projeto:

```bash
unzip agentes-financeiro-app.zip -d /caminho/onde/fica/o/projeto
```

Nada aqui sobrescreve código existente. `pom.xml` **não** é tocado — as adições
estão documentadas em `docs/QUALIDADE-BUILD.md` para você colar.

## O que vem

```
financeiro-app/
├── CLAUDE.md                                    contexto de todas as sessões
├── .claude/
│   ├── agents/                                  os 7 agentes
│   │   ├── arquiteto.md
│   │   ├── dominio-financeiro.md
│   │   ├── seguranca.md
│   │   ├── qa-e-revisao.md
│   │   ├── java-backend.md
│   │   ├── angular-frontend.md
│   │   └── documentacao.md
│   └── skills/glossario-financeiro/SKILL.md     fórmulas da planilha
├── docs/
│   ├── AUDITORIA-INICIAL.md                     achados do código atual
│   └── QUALIDADE-BUILD.md                       snippets do pom.xml
├── config/checkstyle/
│   ├── checkstyle.xml
│   └── suppressions.xml
└── backend/financeiro-api/src/test/java/br/com/financeiro/api/arquitetura/
    └── ArquiteturaTest.java
```

## Primeiros passos, em ordem

**1. Formatação em commit isolado** — antes de qualquer trabalho dos agentes:

```bash
cd backend/financeiro-api
mvn spotless:apply && git commit -am "chore: aplica formatacao com spotless"
```

O repositório tem finais de linha misturados. Sem isso, o primeiro PR vem com
centenas de linhas de ruído.

**2. Adicionar os plugins ao `pom.xml`** — ver `docs/QUALIDADE-BUILD.md`.

**3. Ler `docs/AUDITORIA-INICIAL.md`.** Os quatro primeiros itens são o
backlog imediato.

**4. Primeira tarefa sugerida:**

```
Use o seguranca para desenhar a autenticação JWT.
```

É o item de maior gravidade, e o padrão que sair dele é herdado por todo módulo
novo.

## Ordem sugerida de trabalho

1. `seguranca` desenha a autenticação → `arquiteto` define a migração →
   `java-backend` executa
2. `qa-e-revisao` cobre saldo e dashboard
3. `dominio-financeiro` especifica orçamento (destrava o PREVISTO) e a dimensão
   receita ativa/passiva → `java-backend` implementa
4. Independenciômetro no dashboard
5. `angular-frontend` entra quando o backend estabilizar

## Ajuste esperado

As `description` dos agentes decidem o roteamento automático, e isso só se
valida no uso. Se um agente nunca for chamado, ou for chamado no lugar de
outro, edite a `description` — é ajuste normal, não sinal de erro.

Você pode sempre invocar explicitamente: *"use o dominio-financeiro para..."*.

## Os cinco agentes que ficaram de fora

`mobile`, `po-requisitos`, `ux-ui`, `devops` e `integracao` foram adiados de
propósito — cada um custa um arquivo para voltar. A ideia é decidir sobre eles
depois do controle financeiro fechado, quando você tiver visto na prática
quais dos sete são realmente usados.
