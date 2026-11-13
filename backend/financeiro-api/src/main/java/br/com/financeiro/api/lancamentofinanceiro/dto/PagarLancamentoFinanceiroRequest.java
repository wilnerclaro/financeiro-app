package br.com.financeiro.api.lancamentofinanceiro.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PagarLancamentoFinanceiroRequest(
    @NotNull(message = "A data de pagamento é obrigatória.") LocalDate dataPagamento) {}
