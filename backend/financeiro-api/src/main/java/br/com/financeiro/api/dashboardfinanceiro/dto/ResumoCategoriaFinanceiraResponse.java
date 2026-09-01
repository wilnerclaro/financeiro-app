package br.com.financeiro.api.dashboardfinanceiro.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ResumoCategoriaFinanceiraResponse(
    UUID categoriaId,
    String categoriaNome,
    BigDecimal total,
    BigDecimal percentual,
    Long quantidadeLancamentos) {}
