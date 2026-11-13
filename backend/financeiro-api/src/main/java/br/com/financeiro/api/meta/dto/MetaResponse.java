package br.com.financeiro.api.meta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MetaResponse(
    UUID id,
    String nome,
    BigDecimal valorNecessario,
    BigDecimal investimentoInicial,
    boolean ativa,
    @Schema(
            description =
                "Investimento inicial + todos os aportes e juros (E7 = D7 + SUM(F7:AC7)).")
        BigDecimal acumulado,
    @Schema(description = "Acumulado / necessário, fração (1.0 = 100%).")
        BigDecimal percentualAtingido,
    @Schema(description = "Quanto falta para a necessidade; 0 se atingiu.") BigDecimal falta,
    boolean atingida,
    @Schema(description = "Aportes e juros dos 12 meses do ano pedido.") List<MesDaMeta> meses) {

  public record MesDaMeta(Integer mes, BigDecimal aporte, BigDecimal juros) {}
}
