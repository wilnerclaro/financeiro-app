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

## Por que o pom.xml não veio pronto

Seu `pom.xml` pode ter mudado desde o snapshot analisado, e sobrescrevê-lo
arriscaria perder alteração sua. Os blocos abaixo são para colar. São dois:
`<properties>` e `<build><plugins>`, mais uma dependência de teste.

---

## 1. Dependência do ArchUnit

Em `<dependencies>`:

```xml
<dependency>
  <groupId>com.tngtech.archunit</groupId>
  <artifactId>archunit-junit5</artifactId>
  <version>1.5.1</version>
  <scope>test</scope>
</dependency>
```

---

## 2. Properties

Em `<properties>`:

```xml
<checkstyle.config.file>${project.basedir}/../../config/checkstyle/checkstyle.xml</checkstyle.config.file>
<checkstyle.suppressions.file>${project.basedir}/../../config/checkstyle/suppressions.xml</checkstyle.suppressions.file>
```

> Os `../../` sobem de `backend/financeiro-api/` para a raiz. Se você mover o
> módulo, ajuste.

---

## 3. Plugins

Em `<build><plugins>`:

```xml
<!-- Formatacao: fonte unica de verdade, sem discussao de estilo -->
<plugin>
  <groupId>com.diffplug.spotless</groupId>
  <artifactId>spotless-maven-plugin</artifactId>
  <version>3.11.0</version>
  <configuration>
    <java>
      <googleJavaFormat>
        <version>1.34.1</version>
        <style>GOOGLE</style>
      </googleJavaFormat>
      <removeUnusedImports/>
      <trimTrailingWhitespace/>
      <endWithNewline/>
    </java>
    <lineEndings>UNIX</lineEndings>
  </configuration>
  <executions>
    <execution>
      <goals><goal>check</goal></goals>
      <phase>validate</phase>
    </execution>
  </executions>
</plugin>

<!-- Estilo e defeito: conjunto pequeno, ver config/checkstyle -->
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-checkstyle-plugin</artifactId>
  <version>3.6.0</version>
  <dependencies>
    <dependency>
      <groupId>com.puppycrawl.tools</groupId>
      <artifactId>checkstyle</artifactId>
      <version>14.4.0</version>
    </dependency>
  </dependencies>
  <configuration>
    <configLocation>${checkstyle.config.file}</configLocation>
    <propertyExpansion>checkstyle.suppressions.file=${checkstyle.suppressions.file}</propertyExpansion>
    <consoleOutput>true</consoleOutput>
    <failOnViolation>true</failOnViolation>
    <violationSeverity>error</violationSeverity>
    <includeTestSourceDirectory>false</includeTestSourceDirectory>
  </configuration>
  <executions>
    <execution>
      <id>checkstyle-validacao</id>
      <phase>validate</phase>
      <goals><goal>check</goal></goals>
    </execution>
  </executions>
</plugin>

<!-- Cobertura: rigoroso no codigo novo, legado excluido temporariamente -->
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.16</version>
  <configuration>
    <excludes>
      <!-- Sem logica: nada a cobrir -->
      <exclude>**/dto/**</exclude>
      <exclude>**/entity/**</exclude>
      <exclude>**/enums/**</exclude>
      <exclude>**/docs/**</exclude>
      <exclude>**/config/**</exclude>
      <exclude>**/*Application.class</exclude>
      <exclude>**/*MapperImpl.class</exclude>

    </excludes>
  </configuration>
  <executions>
    <execution>
      <id>preparar-agente</id>
      <goals><goal>prepare-agent</goal></goals>
    </execution>
    <execution>
      <id>relatorio</id>
      <phase>test</phase>
      <goals><goal>report</goal></goals>
    </execution>
    <execution>
      <id>verificar-cobertura</id>
      <phase>verify</phase>
      <goals><goal>check</goal></goals>
      <configuration>
        <rules>
          <rule>
            <element>BUNDLE</element>
            <limits>
              <limit>
                <counter>LINE</counter>
                <value>COVEREDRATIO</value>
                <minimum>0.80</minimum>
              </limit>
              <limit>
                <counter>BRANCH</counter>
                <value>COVEREDRATIO</value>
                <minimum>0.70</minimum>
              </limit>
            </limits>
          </rule>
          <rule>
            <element>CLASS</element>
            <limits>
              <limit>
                <counter>LINE</counter>
                <value>COVEREDRATIO</value>
                <minimum>0.60</minimum>
              </limit>
            </limits>
          </rule>
        </rules>
      </configuration>
    </execution>
  </executions>
</plugin>
```

---

## Primeira execução: ordem importa

O Spotless vai reformatar quase todos os arquivos, porque o repositório tem
finais de linha misturados — vários em CRLF, outros em LF, e o
`CategoriaController` tem os dois no mesmo arquivo.

Faça isso **antes** de qualquer trabalho dos agentes, em commit isolado:

```bash
cd backend/financeiro-api
mvn spotless:apply
git add -A
git commit -m "chore: aplica formatacao com spotless"
```

Senão, o primeiro PR de feature vem com centenas de linhas de ruído de
formatação misturadas com mudança real, e a revisão fica impossível.

Depois:

```bash
mvn verify
```

Se o Checkstyle acusar algo não previsto em `suppressions.xml`, prefira
corrigir o código. Se for regra que não faz sentido para este projeto, remova-a
do `checkstyle.xml` — mas remova conscientemente, não por incômodo.

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
camadas é unidirecional, service público é transacional, campo monetário não é
ponto flutuante, e nomenclatura por camada.

Se um teste falhar, **corrija o código**. Alterar a regra para acomodar a
violação anula o propósito — e é exatamente assim que arquitetura erode em
silêncio quando o volume de código cresce.
