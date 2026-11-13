package br.com.financeiro.api.meta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record PainelMetasResponse(
    Integer ano,
    @Schema(description = "Soma das necessidades (C19).") BigDecimal totalNecessario,
    @Schema(description = "Soma dos acumulados (E19).") BigDecimal totalAcumulado,
    @Schema(description = "Total acumulado / total necessário, fração.")
        BigDecimal percentualAtingido,
    List<MetaResponse> metas,
    @Schema(description = "Soma de aportes e juros de todas as metas, mês a mês (linha 19).")
        List<MetaResponse.MesDaMeta> totaisPorMes) {}
