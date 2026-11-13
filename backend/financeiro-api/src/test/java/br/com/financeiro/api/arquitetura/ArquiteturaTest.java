package br.com.financeiro.api.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
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
        .and()
        .areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("Service")
        .check(classes);
  }

  @Test
  @DisplayName("classes de repository terminam em Repository (ou Specifications, para filtros)")
  void nomenclaturaDeRepository() {
    classes()
        .that()
        .resideInAPackage("..repository..")
        .and()
        .areTopLevelClasses()
        .should()
        .haveSimpleNameEndingWith("Repository")
        .orShould()
        .haveSimpleNameEndingWith("Specifications")
        .check(classes);
  }

  @Test
  @DisplayName("módulos de negócio não têm dependência circular")
  void modulosSemCiclos() {
    slices()
        .matching(PACOTE_RAIZ + ".(*)..")
        .should()
        .beFreeOfCycles()
        .because(
            "um módulo que depende de outro que depende dele não pode ser extraído nem "
                + "testado isoladamente")
        .check(classes);
  }

  @Test
  @DisplayName("service só usa o repository do próprio módulo")
  void serviceNaoUsaRepositoryDeOutroModulo() {
    classes()
        .that()
        .resideInAPackage("..service..")
        .should(usarApenasRepositoryDoProprioModulo())
        .because("o acesso aos dados de outro módulo passa pelo service dele")
        .check(classes);
  }

  private static ArchCondition<JavaClass> usarApenasRepositoryDoProprioModulo() {
    return new ArchCondition<>("usar apenas repository do próprio módulo") {
      @Override
      public void check(JavaClass classe, ConditionEvents eventos) {
        String modulo = moduloDe(classe.getPackageName());
        classe.getDirectDependenciesFromSelf().stream()
            .map(Dependency::getTargetClass)
            .filter(alvo -> alvo.getPackageName().contains(".repository"))
            .filter(alvo -> !moduloDe(alvo.getPackageName()).equals(modulo))
            .distinct()
            .forEach(
                alvo ->
                    eventos.add(
                        SimpleConditionEvent.violated(
                            classe, classe.getName() + " usa " + alvo.getName())));
      }
    };
  }

  private static String moduloDe(String pacote) {
    String resto = pacote.substring(PACOTE_RAIZ.length() + 1);
    int ponto = resto.indexOf('.');
    return ponto < 0 ? resto : resto.substring(0, ponto);
  }
}
