package br.com.financeiro.api.orcamento.dto;

import br.com.financeiro.api.categoria.enums.TipoCategoria;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ResumoAnualOrcamentoResponse(Integer ano, List<Linha> linhas) {

  public record Linha(
      UUID categoriaId,
      String nome,
      UUID categoriaPaiId,
      TipoCategoria tipo,
      BigDecimal totalPrevisto,
      BigDecimal totalRealizado,
      @Schema(description = "Média dos meses com previsto informado.") BigDecimal mediaPrevisto,
      @Schema(description = "Média dos meses com realizado.") BigDecimal mediaRealizado,
      @Schema(description = "Previsto e realizado de janeiro a dezembro (12 posições).")
          List<Mes> meses) {}

  public record Mes(Integer mes, BigDecimal previsto, BigDecimal realizado) {}
}
