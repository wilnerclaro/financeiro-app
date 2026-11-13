package br.com.financeiro.api.dashboardfinanceiro.projection;

import java.math.BigDecimal;

public interface ResumoMensalFinanceiroProjection {

  BigDecimal getTotalReceitas();

  BigDecimal getTotalDespesas();

  Long getQuantidadeLancamentos();
}
