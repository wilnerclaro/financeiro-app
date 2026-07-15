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
        StatusLancamentoFinanceiro status,
        String observacao,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
}