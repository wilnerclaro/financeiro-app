package br.com.financeiro.api.orcamento.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public record RepetirPrevistoRequest(
    @NotNull(message = "A categoria é obrigatória.") UUID categoriaId,
    @NotNull(message = "O valor previsto é obrigatório.")
        @DecimalMin(value = "0.00", message = "O valor previsto não pode ser negativo.")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O valor previsto deve ter no máximo 17 dígitos inteiros e 2 casas decimais.")
        BigDecimal valorPrevisto,
    @ArraySchema(
            arraySchema = @Schema(description = "Meses que recebem o valor, no formato ano-mês."),
            schema = @Schema(type = "string", example = "2026-11"))
        @NotEmpty(message = "Informe ao menos um mês.")
        @Size(max = 24, message = "Informe no máximo 24 meses.")
        List<@NotNull(message = "Mês inválido.") YearMonth> meses,
    @Schema(description = "true substitui o previsto que já existir; false mantém.")
        boolean substituirExistentes) {}
