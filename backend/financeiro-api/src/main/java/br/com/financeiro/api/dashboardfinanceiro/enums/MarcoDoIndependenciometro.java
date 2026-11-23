package br.com.financeiro.api.dashboardfinanceiro.enums;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

public enum MarcoDoIndependenciometro {
  DEZ_POR_CENTO("0.10"),
  VINTE_POR_CENTO("0.20"),
  TRINTA_POR_CENTO("0.30"),
  CINQUENTA_POR_CENTO("0.50"),
  INDEPENDENCIA_FINANCEIRA("1.00");

  private static final BigDecimal SEM_FAIXA = new BigDecimal("0.00");

  private final BigDecimal percentual;

  MarcoDoIndependenciometro(String percentual) {
    this.percentual = new BigDecimal(percentual);
  }

  public BigDecimal getPercentual() {
    return percentual;
  }

  public static Optional<MarcoDoIndependenciometro> proximo(BigDecimal indice) {
    return Arrays.stream(values()).filter(marco -> !marco.foiAtingido(indice)).findFirst();
  }

  public static BigDecimal faixa(BigDecimal indice) {
    return Arrays.stream(values())
        .filter(marco -> marco != INDEPENDENCIA_FINANCEIRA && marco.foiAtingido(indice))
        .reduce((anterior, seguinte) -> seguinte)
        .map(MarcoDoIndependenciometro::getPercentual)
        .orElse(SEM_FAIXA);
  }

  private boolean foiAtingido(BigDecimal indice) {
    return indice.compareTo(percentual) >= 0;
  }
}
