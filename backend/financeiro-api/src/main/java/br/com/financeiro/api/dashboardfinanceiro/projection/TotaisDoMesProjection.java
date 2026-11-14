package br.com.financeiro.api.dashboardfinanceiro.projection;

import java.math.BigDecimal;

public interface TotaisDoMesProjection {
  Integer getAno();

  Integer getMes();

  BigDecimal getReceitas();

  BigDecimal getDespesas();

  BigDecimal getReceitaPassiva();
}
