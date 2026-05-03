package br.com.financeiro.api.conta.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CorrigirSaldoInicialContaRequest(
        @NotNull(message = "O usuário é obrigatório.")
        UUID usuarioId,

        @NotNull(message = "O saldo inicial é obrigatório.")
        @Digits(integer = 17, fraction = 2, message = "O saldo inicial deve ter no máximo 17 dígitos inteiros e 2 casas decimais.")
        BigDecimal saldoInicial
) {
}
