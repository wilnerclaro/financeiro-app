# Qualidade de build

O build é a autoridade. Regra que não quebra o build é sugestão, e sugestão não
sobrevive a volume de código gerado.

```
mvn verify
```

roda Spotless, Checkstyle, JaCoCo e ArchUnit.

---

## Por que o projeto usa Java 21 e não Java 25

O projeto começou em Java 25, mas o Spotless (via google-java-format) não roda
sob JVM 25 — `NoSuchMethodError` em `Log$DeferredDiagnosticHandler.getDiagnostics()`,
porque a assinatura interna do `javac` mudou entre 21 e 25. É um
[bug aberto no Spotless](https://github.com/diffplug/spotless/issues/2468),
sem correção lançada em `google-java-format` até a criação deste documento.
O JaCoCo 0.8.12 tinha o mesmo tipo de problema com bytecode de Java 25/24
(resolvido subindo para 0.8.13, que já está no `pom.xml`).

A alternativa seria compilar em 25 via toolchain enquanto o Maven roda sob 21
só para as ferramentas — mas isso significa manter dois JDKs sincronizados em
toda máquina de dev e no CI, para contornar um bug temporário de terceiros.
Não valeu a pena: o projeto recuou para Java 21 (LTS, sem essas
incompatibilidades). Se o google-java-format lançar suporte a JDK 25, revisitar.

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
  <version>1.3.0</version>
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
  <version>2.44.0</version>
  <configuration>
    <java>
      <googleJavaFormat>
        <version>1.24.0</version>
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
      <version>10.20.1</version>
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
  <version>0.8.12</version>
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

      <!--
        ================== DIVIDA TECNICA ==================
        Modulos anteriores ao gate de cobertura. Cada um sai desta lista
        conforme o qa-e-revisao cobrir.

        NENHUM MODULO NOVO ENTRA AQUI. Codigo novo nasce coberto.
      -->
      <exclude>**/categoria/service/**</exclude>
      <exclude>**/conta/service/**</exclude>
      <exclude>**/lancamentofinanceiro/service/**</exclude>
      <exclude>**/dashboardfinanceiro/service/**</exclude>
      <exclude>**/categoria/controller/**</exclude>
      <exclude>**/conta/controller/**</exclude>
      <exclude>**/lancamentofinanceiro/controller/**</exclude>
      <exclude>**/dashboardfinanceiro/controller/**</exclude>
      <!-- ==================================================== -->
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

O projeto começa em 0%. O gate global de 80% só é alcançável porque os módulos
legados estão excluídos: a métrica incide sobre o que for escrito de agora em
diante.

Isso é intencional. Um gate que já nasce vermelho ensina o time a pular o gate.

**A regra que sustenta tudo**: nenhum módulo novo entra na lista de exclusão.
Cada módulo legado que sai da lista é progresso mensurável, e é bom sinal ver
essa lista encolher a cada PR.

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
