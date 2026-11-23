package br.com.financeiro.api.conta.dto;

import br.com.financeiro.api.conta.enums.TipoConta;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarContaRequest(
    @NotBlank(message = "O nome da conta é obrigatório.")
        @Size(max = 100, message = "O nome da conta deve ter no máximo 100 caracteres.")
        String nome,
    @NotNull(message = "O tipo da conta é obrigatório.") TipoConta tipo,
    @Min(value = 1, message = "O dia de fechamento deve estar entre 1 e 28.")
        @Max(value = 28, message = "O dia de fechamento deve estar entre 1 e 28.")
        Integer diaFechamento,
    @Min(value = 1, message = "O dia de vencimento deve estar entre 1 e 28.")
        @Max(value = 28, message = "O dia de vencimento deve estar entre 1 e 28.")
        Integer diaVencimento) {}
