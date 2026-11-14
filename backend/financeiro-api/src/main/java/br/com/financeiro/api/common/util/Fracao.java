package br.com.financeiro.api.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Fracao {

  public static final int ESCALA = 4;
  public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(ESCALA);

  private Fracao() {}

  public static BigDecimal de(BigDecimal numerador, BigDecimal denominador) {
    if (numerador == null || denominador == null || denominador.signum() == 0) {
      return ZERO;
    }
    return numerador.divide(denominador, ESCALA, RoundingMode.HALF_UP);
  }

  public static BigDecimal evolucao(BigDecimal atual, BigDecimal anterior) {
    return de(atual.subtract(anterior), anterior);
  }
}
