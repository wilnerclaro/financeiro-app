package br.com.financeiro.api.orcamento.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.YearMonth;
import java.util.List;

public record CopiarPrevistoRequest(
    @ArraySchema(
            arraySchema = @Schema(description = "Meses de destino, no formato ano-mês."),
            schema = @Schema(type = "string", example = "2026-11"))
        @NotEmpty(message = "Informe ao menos um mês.")
        @Size(max = 24, message = "Informe no máximo 24 meses.")
        List<@NotNull(message = "Mês inválido.") YearMonth> meses,
    @Schema(description = "true substitui o previsto que já existir; false mantém.")
        boolean substituirExistentes) {}
