package br.com.financeiro.api.orcamento.dto;

import br.com.financeiro.api.categoria.enums.FonteReceita;
import br.com.financeiro.api.categoria.enums.TipoCategoria;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse.Valores;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ResumoAnualOrcamentoResponse(
    Integer ano,
    @Schema(description = "Cada categoria com os próprios valores, sem somar as subcategorias.")
        List<Linha> linhas,
    @Schema(
            description =
                "Grupos (categorias sem pai) com a soma das subcategorias, receitas e despesas.")
        List<Linha> grupos,
    @Schema(description = "Totais realizados do ano.") Indicadores indicadores,
    @Schema(description = "Totais de cada mês, de janeiro a dezembro (12 posições).")
        List<TotaisDoMes> meses) {

  public record Linha(
      UUID categoriaId,
      String nome,
      UUID categoriaPaiId,
      TipoCategoria tipo,
      FonteReceita fonteReceita,
      BigDecimal totalPrevisto,
      BigDecimal totalRealizado,
      @Schema(description = "Média dos meses com previsto informado.") BigDecimal mediaPrevisto,
      @Schema(description = "Média dos meses com realizado.") BigDecimal mediaRealizado,
      @Schema(description = "Previsto e realizado de janeiro a dezembro (12 posições).")
          List<Mes> meses) {}

  public record Mes(
      Integer mes,
      BigDecimal previsto,
      BigDecimal realizado,
      @Schema(description = "Realizado / previsto, fração (1.0 = 100%). 0 se não há previsto.")
          BigDecimal percentualRealizado) {}

  public record Indicadores(
      @Schema(description = "Receitas realizadas no ano.") BigDecimal entrou,
      @Schema(description = "Despesas realizadas no ano.") BigDecimal saiu,
      @Schema(description = "Entrou menos saiu.") BigDecimal guardou,
      @Schema(description = "Guardou / entrou, fração. 0 sem receita.")
          BigDecimal percentualGuardado,
      @Schema(description = "Média das receitas dos meses com receita.") BigDecimal mediaEntrou,
      @Schema(description = "Média das despesas dos meses com despesa.") BigDecimal mediaSaiu,
      @Schema(description = "Receitas previstas no ano.") BigDecimal previstoReceitas,
      @Schema(description = "Despesas previstas no ano.") BigDecimal previstoDespesas) {}

  public record TotaisDoMes(
      Integer mes,
      Valores receitas,
      Valores receitasFonteAtiva,
      Valores receitasFontePassiva,
      Valores despesas,
      @Schema(description = "Receitas menos despesas realizadas.") BigDecimal guardou,
      @Schema(description = "Guardou / receitas realizadas, fração. 0 sem receita.")
          BigDecimal percentualGuardado,
      @Schema(
              description =
                  "Despesa realizada contra o mês anterior, fração (0 se o anterior não teve "
                      + "despesa). Nulo em janeiro e em mês sem despesa realizada.")
          BigDecimal evolucaoDespesas,
      @Schema(description = "Receita passiva / despesa realizadas, fração. 0 sem despesa.")
          BigDecimal independenciometro) {}
}
