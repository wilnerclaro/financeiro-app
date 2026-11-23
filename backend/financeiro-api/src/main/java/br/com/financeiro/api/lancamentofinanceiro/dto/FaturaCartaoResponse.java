package br.com.financeiro.api.lancamentofinanceiro.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FaturaCartaoResponse(
    UUID contaId,
    String nomeConta,
    @Schema(description = "Ano do vencimento da fatura.") Integer ano,
    @Schema(description = "Mês do vencimento da fatura.") Integer mes,
    LocalDate dataFechamento,
    LocalDate dataVencimento,
    @Schema(description = "Verdadeiro quando a data de fechamento já passou.") boolean fechada,
    @Schema(description = "Despesas menos créditos da fatura (o valor a pagar).") BigDecimal total,
    List<LancamentoFinanceiroResponse> lancamentos) {}
