package br.com.financeiro.api.dashboardfinanceiro.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ResumoMensalFinanceiroResponse(
    UUID usuarioId,
    Integer ano,
    Integer mes,
    BigDecimal totalReceitas,
    BigDecimal totalDespesas,
    BigDecimal saldoPeriodo,
    Long quantidadeLancamentos,
    List<ResumoCategoriaFinanceiraResponse> receitasPorCategoria,
    List<ResumoCategoriaFinanceiraResponse> despesasPorCategoria) {}
