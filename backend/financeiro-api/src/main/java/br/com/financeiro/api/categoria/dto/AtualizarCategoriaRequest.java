package br.com.financeiro.api.categoria.dto;

import br.com.financeiro.api.categoria.enums.TipoCategoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AtualizarCategoriaRequest(
    @NotNull(message = "O usuário é obrigatório.") UUID usuarioId,
    UUID categoriaPaiId,
    @NotBlank(message = "O nome da categoria é obrigatório.")
        @Size(max = 100, message = "O nome da categoria deve ter no máximo 100 caracteres.")
        String nome,
    @NotNull(message = "O tipo da categoria é obrigatório.") TipoCategoria tipo,
    @NotNull(message = "O status ativo da categoria é obrigatório.") Boolean ativa) {}
