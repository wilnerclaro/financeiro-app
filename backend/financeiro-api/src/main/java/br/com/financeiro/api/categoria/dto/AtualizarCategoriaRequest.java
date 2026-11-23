package br.com.financeiro.api.categoria.dto;

import br.com.financeiro.api.categoria.enums.FonteReceita;
import br.com.financeiro.api.categoria.enums.TipoCategoria;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AtualizarCategoriaRequest(
    UUID categoriaPaiId,
    @NotBlank(message = "O nome da categoria é obrigatório.")
        @Size(max = 100, message = "O nome da categoria deve ter no máximo 100 caracteres.")
        String nome,
    @NotNull(message = "O tipo da categoria é obrigatório.") TipoCategoria tipo,
    @Schema(
            description =
                "Só para receita: ATIVA (padrão) ou PASSIVA. Subcategoria herda a do pai; "
                    + "em despesa é ignorado.")
        FonteReceita fonteReceita) {}
