package br.com.financeiro.api.conta.dto;

import br.com.financeiro.api.conta.enums.TipoConta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AtualizarContaRequest(
        @NotNull(message = "O usuário é obrigatório.")
        UUID usuarioId,

        @NotBlank(message = "O nome da conta é obrigatório.")
        @Size(max = 100, message = "O nome da conta deve ter no máximo 100 caracteres.")
        String nome,

        @NotNull(message = "O tipo da conta é obrigatório.")
        TipoConta tipo,

        @NotNull(message = "O status ativo da conta é obrigatório.")
        Boolean ativa
) {
}
