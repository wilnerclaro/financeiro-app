package br.com.financeiro.api.dashboardfinanceiro.dto;

import br.com.financeiro.api.lancamentofinanceiro.dto.LancamentoFinanceiroResponse;
import br.com.financeiro.api.lancamentofinanceiro.dto.LancamentoRapidoResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InicioResponse(
    Integer ano,
    Integer mes,
    @Schema(description = "Receitas pagas no mês (competência).") BigDecimal entrou,
    @Schema(description = "Despesas pagas no mês (competência).") BigDecimal saiu,
    @Schema(description = "Entrou − saiu.") BigDecimal sobrou,
    @Schema(description = "Sobrou / entrou, fração (1.0 = 100%).") BigDecimal percentualGuardado,
    @Schema(description = "Guardar 30% da receita.") MetaDeGuardar meta30,
    @Schema(description = "Guardar 50% da receita.") MetaDeGuardar meta50,
    ComparativoMesAnterior comparativoMesAnterior,
    OrcamentoDoMes orcamento,
    Independenciometro independenciometro,
    @Schema(description = "O mês pedido e os 5 anteriores, do mais antigo ao mais recente.")
        List<MesDaEvolucao> evolucao,
    ParaPagar paraPagar,
    @Schema(description = "Despesas com 90% ou mais do previsto já realizado no mês.")
        List<CategoriaComPrevisto> categoriasAcimaDoPrevisto,
    @Schema(description = "Receitas que já passaram do previsto no mês.")
        List<CategoriaComPrevisto> receitasAcimaDoPrevisto,
    @Schema(description = "Os 5 lançamentos mais recentes, sem os cancelados.")
        List<LancamentoFinanceiroResponse> ultimosLancamentos,
    LancamentoRapidoResponse lancamentoRapido,
    @Schema(description = "Saldo das contas ativas, sem os cartões de crédito.")
        BigDecimal saldoEmContas) {

  public record MetaDeGuardar(
      BigDecimal percentual,
      @Schema(description = "Receita do mês × percentual.") BigDecimal valor,
      boolean atingiu,
      @Schema(description = "Quanto ainda falta guardar; 0 se atingiu.") BigDecimal falta) {}

  public record ComparativoMesAnterior(
      @Schema(description = "Receitas do mês − receitas do mês anterior.")
          BigDecimal diferencaReceitas,
      @Schema(description = "Variação das receitas, fração.") BigDecimal evolucaoReceitas,
      @Schema(description = "Despesas do mês − despesas do mês anterior; negativo = gastou menos.")
          BigDecimal diferencaDespesas,
      @Schema(description = "Variação das despesas, fração.") BigDecimal evolucaoDespesas) {}

  public record OrcamentoDoMes(
      BigDecimal receitasPrevistas,
      BigDecimal despesasPrevistas,
      @Schema(description = "Despesa realizada / despesa prevista, fração.")
          BigDecimal percentualDespesasUsado,
      @Schema(description = "Despesa prevista − realizada; negativo se passou do previsto.")
          BigDecimal restaDasDespesas,
      @Schema(description = "Até 3 grupos de despesa com previsto, do mais usado ao menos usado.")
          List<CategoriaComPrevisto> gruposMaisUsados) {}

  public record Independenciometro(
      BigDecimal indice,
      BigDecimal receitaPassiva,
      BigDecimal despesaTotal,
      BigDecimal indiceMesAnterior,
      @Schema(description = "Índice do mês − índice do mês anterior (pontos de fração).")
          BigDecimal variacao,
      @Schema(description = "Faixa atingida: 0, 0.10, 0.20, 0.30 ou 0.50.") BigDecimal faixa,
      @Schema(description = "Próximo marco (0.10, 0.20, 0.30, 0.50 ou 1.00); nulo acima de 1.")
          BigDecimal proximoMarco,
      @Schema(description = "Receita passiva que falta por mês para chegar ao próximo marco.")
          BigDecimal faltaParaProximoMarco) {}

  public record MesDaEvolucao(
      Integer ano,
      Integer mes,
      BigDecimal entrou,
      BigDecimal saiu,
      BigDecimal sobrou,
      @Schema(description = "Sobrou / entrou, fração.") BigDecimal percentualGuardado,
      @Schema(description = "Guardou 30% ou mais do que entrou.") boolean atingiuMeta30,
      BigDecimal receitaPassiva,
      BigDecimal independenciometro,
      @Schema(description = "Variação das receitas contra o mês anterior, fração.")
          BigDecimal evolucaoReceitas,
      @Schema(description = "Variação das despesas contra o mês anterior, fração.")
          BigDecimal evolucaoDespesas) {}

  public record ParaPagar(
      @Schema(description = "Soma dos pendentes vencidos ou que vencem até o fim do mês.")
          BigDecimal total,
      int quantidade,
      @Schema(description = "Até 10 desses pendentes, atrasados primeiro.")
          List<LancamentoFinanceiroResponse> itens) {}

  public record CategoriaComPrevisto(
      UUID categoriaId,
      String nome,
      @Schema(description = "Grupo da categoria; nulo quando ela própria é um grupo.") String grupo,
      BigDecimal previsto,
      BigDecimal realizado,
      @Schema(description = "Realizado − previsto.") BigDecimal desvio,
      BigDecimal percentualRealizado) {}
}
