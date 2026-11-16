package br.com.financeiro.api.lancamentofinanceiro.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record PagarFaturaRequest(
    @Schema(description = "Cartão de crédito dono da fatura.")
        @NotNull(message = "O cartão é obrigatório.")
        UUID contaId,
    @Schema(description = "Ano do vencimento da fatura.") @NotNull(message = "O ano é obrigatório.")
        Integer ano,
    @Schema(description = "Mês do vencimento da fatura.") @NotNull(message = "O mês é obrigatório.")
        Integer mes,
    @Schema(description = "Conta de onde sai o dinheiro do pagamento.")
        @NotNull(message = "A conta do pagamento é obrigatória.")
        UUID contaPagamentoId,
    @NotNull(message = "A data de pagamento é obrigatória.") LocalDate dataPagamento) {}
