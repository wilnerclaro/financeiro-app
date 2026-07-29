package br.com.financeiro.api.dashboardfinanceiro.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface ResumoCategoriaFinanceiraProjection {

    UUID getCategoriaId();

    String getCategoriaNome();

    BigDecimal getTotal();

    Long getQuantidadeLancamentos();
}
