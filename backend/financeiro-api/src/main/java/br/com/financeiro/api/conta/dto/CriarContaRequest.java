package br.com.financeiro.api.conta.dto;

import br.com.financeiro.api.conta.enums.TipoConta;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CriarContaRequest(
    @NotNull(message = "O usuário é obrigatório.") UUID usuarioId,
    @NotBlank(message = "O nome da conta é obrigatório.")
        @Size(max = 100, message = "O nome da conta deve ter no máximo 100 caracteres.")
        String nome,
    @NotNull(message = "O tipo da conta é obrigatório.") TipoConta tipo,
    @NotNull(message = "O saldo inicial é obrigatório.")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O saldo inicial deve ter no máximo 17 dígitos inteiros e 2 casas decimais.")
        BigDecimal saldoInicial) {}
