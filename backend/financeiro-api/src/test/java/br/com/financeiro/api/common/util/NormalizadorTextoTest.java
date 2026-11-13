package br.com.financeiro.api.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NormalizadorTexto")
class NormalizadorTextoTest {

  @Test
  @DisplayName("normalizarNome remove espacos das pontas e colapsa espacos internos")
  void normalizarNomeColapsaEspacos() {
    assertThat(NormalizadorTexto.normalizarNome("  Conta   Nubank  ")).isEqualTo("Conta Nubank");
  }

  @Test
  @DisplayName("normalizarNome preserva acento e caixa")
  void normalizarNomePreservaAcentoECaixa() {
    assertThat(NormalizadorTexto.normalizarNome("Alimentação")).isEqualTo("Alimentação");
  }

  @Test
  @DisplayName("normalizarNome devolve nulo para entrada nula")
  void normalizarNomeNulo() {
    assertThat(NormalizadorTexto.normalizarNome(null)).isNull();
  }

  @Test
  @DisplayName("normalizarParaComparacao remove acento, colapsa espacos e usa minusculas")
  void normalizarParaComparacao() {
    assertThat(NormalizadorTexto.normalizarParaComparacao("  Alimentação   FORA de Casa "))
        .isEqualTo("alimentacao fora de casa");
  }

  @Test
  @DisplayName("normalizarParaComparacao devolve nulo para entrada nula")
  void normalizarParaComparacaoNulo() {
    assertThat(NormalizadorTexto.normalizarParaComparacao(null)).isNull();
  }
}
