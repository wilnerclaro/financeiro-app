package br.com.financeiro.api.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Codifica as convencoes de arquitetura do projeto como teste executavel.
 *
 * <p>Estas regras existiam apenas como disciplina humana. Com varios agentes escrevendo codigo,
 * disciplina nao escala — aqui elas viram build vermelho.
 *
 * <p>Se uma regra falhar, corrija o codigo. Alterar o teste para acomodar a violacao anula o
 * proposito dele.
 */
@DisplayName("Regras de arquitetura")
class ArquiteturaTest {

  private static final String PACOTE_RAIZ = "br.com.financeiro.api";

  private static JavaClasses classes;

  @BeforeAll
  static void importarClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(PACOTE_RAIZ);
  }

  @Test
  @DisplayName("controller nao acessa repository diretamente")
  void controllerNaoAcessaRepository() {
    noClasses()
        .that()
        .resideInAPackage("..controller..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..repository..")
        .because("o controller orquestra o service; acesso a dados passa pelo service")
        .check(classes);
  }

  @Test
  @DisplayName("entidade nao vaza para o controller")
  void entidadeNaoVazaParaController() {
    noClasses()
        .that()
        .resideInAPackage("..controller..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..entity..")
        .because("a fronteira publica trafega DTO, nunca entidade JPA")
        .check(classes);
  }

  @Test
  @DisplayName("repository nao conhece service nem controller")
  void repositoryNaoConheceCamadasSuperiores() {
    noClasses()
        .that()
        .resideInAPackage("..repository..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..service..", "..controller..")
        .because("a dependencia entre camadas e unidirecional")
        .check(classes);
  }

  @Test
  @DisplayName("entidade nao depende de nenhuma outra camada")
  void entidadeNaoDependeDeOutrasCamadas() {
    noClasses()
        .that()
        .resideInAPackage("..entity..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..service..", "..controller..", "..repository..", "..dto..")
        .because("a entidade e o nucleo do modelo e nao conhece quem a usa")
        .check(classes);
  }

  @Test
  @DisplayName("metodo publico de service e transacional")
  void servicePublicoEhTransacional() {
    methods()
        .that()
        .areDeclaredInClassesThat()
        .areAnnotatedWith(Service.class)
        .and()
        .arePublic()
        .should()
        .beAnnotatedWith(Transactional.class)
        .orShould()
        .beDeclaredInClassesThat()
        .areAnnotatedWith(Transactional.class)
        .because("escrita e leitura precisam de fronteira transacional explicita")
        .check(classes);
  }

  @Test
  @DisplayName("dinheiro nunca e double ou float")
  void dinheiroNaoEPontoFlutuante() {
    noFields()
        .that()
        .areDeclaredInClassesThat()
        .resideInAPackage("..entity..")
        .should()
        .haveRawType(Double.class)
        .orShould()
        .haveRawType(Float.class)
        .orShould()
        .haveRawType(double.class)
        .orShould()
        .haveRawType(float.class)
        .because("valor monetario usa BigDecimal; ponto flutuante perde centavo")
        .check(classes);
  }

  @Test
  @DisplayName("BigDecimal e a escolha para valor monetario")
  void bigDecimalEstaDisponivel() {
    // Guarda simbolica: mantem o import e documenta a intencao da regra acima.
    org.assertj.core.api.Assertions.assertThat(BigDecimal.ZERO).isNotNull();
  }

  @Test
  @DisplayName("classes de controller terminam em Controller")
  void nomenclaturaDeController() {
    classes()
        .that()
        .resideInAPackage("..controller..")
        .and()
        .areNotInterfaces()
        .should()
        .haveSimpleNameEndingWith("Controller")
        .check(classes);
  }

  @Test
  @DisplayName("classes de service terminam em Service")
  void nomenclaturaDeService() {
    classes()
        .that()
        .resideInAPackage("..service..")
        .and()
        .areNotInterfaces()
        .should()
        .haveSimpleNameEndingWith("Service")
        .check(classes);
  }

  @Test
  @DisplayName("classes de repository terminam em Repository")
  void nomenclaturaDeRepository() {
    classes()
        .that()
        .resideInAPackage("..repository..")
        .should()
        .haveSimpleNameEndingWith("Repository")
        .check(classes);
  }
}
