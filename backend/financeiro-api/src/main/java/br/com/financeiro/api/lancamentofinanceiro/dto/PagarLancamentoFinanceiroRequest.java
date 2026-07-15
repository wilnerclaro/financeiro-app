package br.com.financeiro.api.lancamentofinanceiro.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record PagarLancamentoFinanceiroRequest(

        @NotNull(message = "O usuario e obrigatorio.")
        UUID usuarioId,

        @NotNull(message = "A data de pagamento e obrigatoria.")
        LocalDate dataPagamento
) {
}
