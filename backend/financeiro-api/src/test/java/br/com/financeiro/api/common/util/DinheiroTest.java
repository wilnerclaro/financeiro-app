package br.com.financeiro.api.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Dinheiro (escala monetária de 2 casas)")
class DinheiroTest {

  @Test
  @DisplayName("arredonda para 2 casas com HALF_UP e trata nulo como zero")
  void arredonda() {
    assertThat(Dinheiro.arredondar(new BigDecimal("10.005"))).isEqualTo(new BigDecimal("10.01"));
    assertThat(Dinheiro.arredondar(new BigDecimal("7"))).isEqualTo(new BigDecimal("7.00"));
    assertThat(Dinheiro.arredondar(null)).isEqualTo(new BigDecimal("0.00"));
  }
}
