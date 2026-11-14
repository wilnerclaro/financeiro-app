package br.com.financeiro.api.meta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public record MovimentoMetaRequest(
    @Schema(description = "Aporte do mês; negativo para resgate. Padrão 0.", example = "500.00")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O valor deve ter no máximo 2 casas decimais.")
        BigDecimal aporte,
    @Schema(
            description = "Juros, dividendos ou variação do mês; pode ser negativo.",
            example = "42.10")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O valor deve ter no máximo 2 casas decimais.")
        BigDecimal juros) {}
