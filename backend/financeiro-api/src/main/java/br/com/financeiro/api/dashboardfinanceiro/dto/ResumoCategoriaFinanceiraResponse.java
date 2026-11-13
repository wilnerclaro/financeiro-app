package br.com.financeiro.api.dashboardfinanceiro.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

public record ResumoCategoriaFinanceiraResponse(
    UUID categoriaId,
    String categoriaNome,
    BigDecimal total,
    @Schema(
            description =
                "Participação da categoria no total do tipo, como fração de 0 a 1 (0.2500 = 25%).",
            example = "0.2500")
        BigDecimal percentual,
    Long quantidadeLancamentos) {}
