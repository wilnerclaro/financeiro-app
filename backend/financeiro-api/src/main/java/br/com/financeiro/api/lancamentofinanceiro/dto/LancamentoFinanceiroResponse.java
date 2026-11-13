package br.com.financeiro.api.lancamentofinanceiro.dto;

import br.com.financeiro.api.lancamentofinanceiro.enums.StatusLancamentoFinanceiro;
import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LancamentoFinanceiroResponse(
    UUID id,
    UUID usuarioId,
    UUID contaId,
    String nomeConta,
    UUID categoriaId,
    String nomeCategoria,
    String descricao,
    TipoLancamentoFinanceiro tipo,
    BigDecimal valor,
    LocalDate dataCompetencia,
    LocalDate dataVencimento,
    LocalDate dataPagamento,
    LocalDate dataCompra,
    StatusLancamentoFinanceiro status,
    String observacao,
    OffsetDateTime criadoEm,
    OffsetDateTime atualizadoEm) {

  public LancamentoFinanceiroResponse comStatus(StatusLancamentoFinanceiro novoStatus) {
    return new LancamentoFinanceiroResponse(
        id,
        usuarioId,
        contaId,
        nomeConta,
        categoriaId,
        nomeCategoria,
        descricao,
        tipo,
        valor,
        dataCompetencia,
        dataVencimento,
        dataPagamento,
        dataCompra,
        novoStatus,
        observacao,
        criadoEm,
        atualizadoEm);
  }
}
