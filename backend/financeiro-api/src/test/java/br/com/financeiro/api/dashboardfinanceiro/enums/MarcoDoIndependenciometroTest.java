package br.com.financeiro.api.dashboardfinanceiro.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MarcoDoIndependenciometro (faixa e próximo marco)")
class MarcoDoIndependenciometroTest {

  @Test
  @DisplayName("faixa é o maior marco atingido, sem passar de 50%")
  void faixa() {
    assertThat(MarcoDoIndependenciometro.faixa(new BigDecimal("0.0500"))).isEqualByComparingTo("0");
    assertThat(MarcoDoIndependenciometro.faixa(new BigDecimal("0.3333")))
        .isEqualByComparingTo("0.30");
    assertThat(MarcoDoIndependenciometro.faixa(new BigDecimal("0.5000")))
        .isEqualByComparingTo("0.50");
    assertThat(MarcoDoIndependenciometro.faixa(new BigDecimal("1.2000")))
        .isEqualByComparingTo("0.50");
  }

  @Test
  @DisplayName("próximo marco é o primeiro acima do índice; nenhum a partir de 100%")
  void proximo() {
    assertThat(MarcoDoIndependenciometro.proximo(new BigDecimal("0.3333")))
        .contains(MarcoDoIndependenciometro.CINQUENTA_POR_CENTO);
    assertThat(MarcoDoIndependenciometro.proximo(BigDecimal.ZERO))
        .contains(MarcoDoIndependenciometro.DEZ_POR_CENTO);
    assertThat(MarcoDoIndependenciometro.proximo(new BigDecimal("0.9999")))
        .contains(MarcoDoIndependenciometro.INDEPENDENCIA_FINANCEIRA);
    assertThat(MarcoDoIndependenciometro.proximo(BigDecimal.ONE)).isEmpty();
  }
}
