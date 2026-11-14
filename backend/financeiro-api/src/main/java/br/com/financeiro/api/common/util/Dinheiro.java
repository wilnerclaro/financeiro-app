package br.com.financeiro.api.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Dinheiro {

  public static final int ESCALA = 2;
  public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(ESCALA);

  private Dinheiro() {}

  public static BigDecimal arredondar(BigDecimal valor) {
    return valor == null ? ZERO : valor.setScale(ESCALA, RoundingMode.HALF_UP);
  }
}
