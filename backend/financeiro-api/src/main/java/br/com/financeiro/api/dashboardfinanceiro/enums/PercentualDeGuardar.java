package br.com.financeiro.api.dashboardfinanceiro.enums;

import java.math.BigDecimal;

public enum PercentualDeGuardar {
  TRINTA_POR_CENTO("0.30"),
  CINQUENTA_POR_CENTO("0.50");

  private final BigDecimal percentual;

  PercentualDeGuardar(String percentual) {
    this.percentual = new BigDecimal(percentual);
  }

  public BigDecimal getPercentual() {
    return percentual;
  }
}
