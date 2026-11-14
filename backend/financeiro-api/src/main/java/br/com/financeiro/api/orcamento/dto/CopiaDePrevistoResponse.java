package br.com.financeiro.api.orcamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.YearMonth;
import java.util.List;

public record CopiaDePrevistoResponse(List<ResultadoDoMes> meses) {

  public record ResultadoDoMes(
      @Schema(type = "string", example = "2026-11") YearMonth competencia,
      @Schema(description = "Categorias gravadas no mês.") int gravadas,
      @Schema(description = "Categorias que já tinham previsto e foram mantidas.") int mantidas) {}
}
