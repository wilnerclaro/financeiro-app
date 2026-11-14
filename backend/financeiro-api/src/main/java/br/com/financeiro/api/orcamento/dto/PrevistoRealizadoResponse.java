package br.com.financeiro.api.orcamento.dto;

import br.com.financeiro.api.categoria.enums.FonteReceita;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PrevistoRealizadoResponse(
    Integer ano,
    Integer mes,
    Bloco receitas,
    Valores receitasFonteAtiva,
    Valores receitasFontePassiva,
    Bloco despesas,
    @Schema(description = "Receitas menos despesas, previsto e realizado.") Valores saldo) {

  public record Valores(
      BigDecimal previsto,
      BigDecimal realizado,
      BigDecimal desvio,
      @Schema(description = "Realizado / previsto, fração (1.0 = 100%). 0 se não há previsto.")
          BigDecimal percentualRealizado) {}

  public record Bloco(Valores total, List<Grupo> grupos) {}

  public record Grupo(
      UUID categoriaId,
      String nome,
      FonteReceita fonteReceita,
      boolean ativa,
      Valores valores,
      List<Item> subitens) {}

  public record Item(
      UUID categoriaId, String nome, FonteReceita fonteReceita, boolean ativa, Valores valores) {}
}
