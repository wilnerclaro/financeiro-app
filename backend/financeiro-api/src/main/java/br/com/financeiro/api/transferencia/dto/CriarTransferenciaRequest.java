package br.com.financeiro.api.transferencia.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CriarTransferenciaRequest(
    @NotNull(message = "A conta de origem é obrigatória.") UUID contaOrigemId,
    @NotNull(message = "A conta de destino é obrigatória.") UUID contaDestinoId,
    @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O valor deve ter no máximo 17 dígitos inteiros e 2 casas decimais.")
        BigDecimal valor,
    @NotNull(message = "A data da transferência é obrigatória.") LocalDate dataTransferencia,
    @Size(max = 180, message = "A descrição deve ter no máximo 180 caracteres.")
        String descricao) {}
