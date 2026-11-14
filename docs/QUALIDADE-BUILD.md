# Qualidade de build

O build é a autoridade. Regra que não quebra o build é sugestão, e sugestão não
sobrevive a volume de código gerado.

```
mvn verify
```

roda Spotless, Checkstyle, JaCoCo e ArchUnit.

---

## Java 25 e versões das ferramentas

O projeto roda em Java 25 (LTS). Ele chegou a recuar para Java 21 porque o
Spotless, via google-java-format, não rodava sob JVM 25
([issue do Spotless](https://github.com/diffplug/spotless/issues/2468)). Isso
foi resolvido nas versões novas, e a migração para o 25 foi feita em 13/11/2026.

Versões que importam para o JDK 25:

| Ferramenta | Versão | Observação |
| --- | --- | --- |
| google-java-format | 1.34.1 | Mínimo 1.30.0 no JDK 25. A 1.37.0 falha dentro do Spotless 3.11 (`InvocationTargetException`); a 2.0.0 funciona, mas reformata os blocos de texto dos repositories. Subir só com commit isolado de formatação. |
| Spotless | 3.11.0 | |
| JaCoCo | 0.8.16 | Suporte oficial a bytecode do Java 25. |
| ArchUnit | 1.5.1 | A 1.3.0 não lê classes do Java 25. |
| Checkstyle | 14.4.0 | O `checkstyle.xml` atual roda sem ajuste. |

O `main` da aplicação precisa ser `public static void main`. A forma curta do
Java 25 (`static void main`, sem `public`) compila, mas o
`spring-boot-maven-plugin` não encontra a classe principal no `repackage` e o
build falha.

---

## Spotless e Checkstyle

O Spotless (google-java-format) e o Checkstyle rodam na fase `validate`. Para
formatar antes de commitar:

```bash
./mvnw spotless:apply
```

Se o Checkstyle acusar algo, prefira corrigir o código. Se a regra não fizer
sentido para o projeto, remova-a do `config/checkstyle/checkstyle.xml`
conscientemente, não por incômodo.

---

## Sobre o gate de cobertura

Os módulos legados (services e controllers de categoria, conta, lançamento e
dashboard) já saíram da lista de exclusão: eles são cobertos pelos testes de
integração em `src/test/java/br/com/financeiro/api/integracao/`, que sobem a
aplicação contra um PostgreSQL real (Testcontainers) e chamam a API via MockMvc.

Só fica excluído o que não tem lógica (DTO, entidade, enum, docs, config,
mappers gerados).

**A regra que sustenta tudo**: nada entra na lista de exclusão. Código novo
nasce coberto.

## CI

`.github/workflows/ci.yml` roda `./mvnw verify` em todo push para `main` e
`develop` e em todo pull request, com Java 25 (Temurin) e o Docker do runner
para o Testcontainers. O Dependabot (`.github/dependabot.yml`) abre PRs semanais
contra `develop` com atualizações do Maven e mensais das Actions.

---

## ArchUnit

O teste está em
`src/test/java/br/com/financeiro/api/arquitetura/ArquiteturaTest.java` e roda
junto com a suíte.

Ele codifica as convenções que hoje existem só como disciplina: controller não
acessa repository, entidade não vaza para o controller, dependência entre
camadas é unidirecional, service não usa repository de outro módulo, service
público é transacional, campo monetário não é ponto flutuante, e nomenclatura
por camada.

Se um teste falhar, **corrija o código**. Alterar a regra para acomodar a
violação anula o propósito — e é exatamente assim que arquitetura erode em
silêncio quando o volume de código cresce.
