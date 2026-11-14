package br.com.financeiro.api.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Fracao (percentual 0..1 com guarda de divisão por zero)")
class FracaoTest {

  @Test
  @DisplayName("divide com 4 casas e HALF_UP")
  void divide() {
    assertThat(Fracao.de(new BigDecimal("1000"), new BigDecimal("3000")))
        .isEqualByComparingTo("0.3333");
    assertThat(Fracao.de(new BigDecimal("2"), new BigDecimal("3"))).isEqualByComparingTo("0.6667");
  }

  @Test
  @DisplayName("divisão por zero ou nulo devolve 0")
  void guarda() {
    assertThat(Fracao.de(BigDecimal.TEN, BigDecimal.ZERO)).isEqualByComparingTo("0");
    assertThat(Fracao.de(null, BigDecimal.ONE)).isEqualByComparingTo("0");
    assertThat(Fracao.evolucao(BigDecimal.TEN, BigDecimal.ZERO)).isEqualByComparingTo("0");
  }

  @Test
  @DisplayName("evolução = (atual − anterior) / anterior, inclusive negativa")
  void evolucao() {
    assertThat(Fracao.evolucao(new BigDecimal("5000"), new BigDecimal("4000")))
        .isEqualByComparingTo("0.25");
    assertThat(Fracao.evolucao(new BigDecimal("3000"), new BigDecimal("4000")))
        .isEqualByComparingTo("-0.25");
  }
}
