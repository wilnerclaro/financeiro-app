package br.com.financeiro.api.dashboardfinanceiro.service;

import static br.com.financeiro.api.common.util.Dinheiro.ZERO;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.util.Competencia;
import br.com.financeiro.api.common.util.Dinheiro;
import br.com.financeiro.api.common.util.Fracao;
import br.com.financeiro.api.conta.service.ContaService;
import br.com.financeiro.api.dashboardfinanceiro.dto.InicioResponse;
import br.com.financeiro.api.dashboardfinanceiro.dto.InicioResponse.CategoriaComPrevisto;
import br.com.financeiro.api.dashboardfinanceiro.dto.InicioResponse.ComparativoMesAnterior;
import br.com.financeiro.api.dashboardfinanceiro.dto.InicioResponse.Independenciometro;
import br.com.financeiro.api.dashboardfinanceiro.dto.InicioResponse.MesDaEvolucao;
import br.com.financeiro.api.dashboardfinanceiro.dto.InicioResponse.MetaDeGuardar;
import br.com.financeiro.api.dashboardfinanceiro.dto.InicioResponse.OrcamentoDoMes;
import br.com.financeiro.api.dashboardfinanceiro.dto.InicioResponse.ParaPagar;
import br.com.financeiro.api.dashboardfinanceiro.enums.MarcoDoIndependenciometro;
import br.com.financeiro.api.dashboardfinanceiro.enums.PercentualDeGuardar;
import br.com.financeiro.api.dashboardfinanceiro.projection.TotaisDoMesProjection;
import br.com.financeiro.api.dashboardfinanceiro.repository.DashboardFinanceiroRepository;
import br.com.financeiro.api.lancamentofinanceiro.dto.LancamentoFinanceiroResponse;
import br.com.financeiro.api.lancamentofinanceiro.service.LancamentoFinanceiroService;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse.Bloco;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse.Grupo;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse.Item;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse.Valores;
import br.com.financeiro.api.orcamento.service.OrcamentoService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InicioService {

  private static final BigDecimal PERCENTUAL_DE_ALERTA_DO_PREVISTO = new BigDecimal("0.90");
  private static final int MESES_DA_EVOLUCAO = 6;
  private static final int LIMITE_PARA_PAGAR = 10;
  private static final int LIMITE_ULTIMOS = 5;
  private static final int LIMITE_GRUPOS = 3;

  private final DashboardFinanceiroRepository dashboardFinanceiroRepository;
  private final LancamentoFinanceiroService lancamentoFinanceiroService;
  private final ContaService contaService;
  private final OrcamentoService orcamentoService;
  private final Clock clock;

  @Transactional(readOnly = true)
  public InicioResponse montar(UUID usuarioId, Integer ano, Integer mes) {
    YearMonth competencia = competencia(ano, mes, LocalDate.now(clock));

    List<MesDaEvolucao> evolucao = montarEvolucao(usuarioId, competencia);
    MesDaEvolucao atual = evolucao.getLast();
    MesDaEvolucao anterior = evolucao.get(evolucao.size() - 2);

    PrevistoRealizadoResponse orcamento =
        orcamentoService.previstoRealizado(
            usuarioId, competencia.getYear(), competencia.getMonthValue());

    return new InicioResponse(
        competencia.getYear(),
        competencia.getMonthValue(),
        atual.entrou(),
        atual.saiu(),
        atual.sobrou(),
        atual.percentualGuardado(),
        meta(PercentualDeGuardar.TRINTA_POR_CENTO, atual.entrou(), atual.sobrou()),
        meta(PercentualDeGuardar.CINQUENTA_POR_CENTO, atual.entrou(), atual.sobrou()),
        comparativo(atual, anterior),
        orcamentoDoMes(orcamento, atual.saiu()),
        independenciometro(atual, anterior),
        evolucao.subList(1, evolucao.size()),
        paraPagar(usuarioId, competencia),
        filtrarSubitens(orcamento.despesas(), InicioService::chegouAoLimiteDoAlerta),
        filtrarSubitens(orcamento.receitas(), InicioService::passouDoPrevisto),
        lancamentoFinanceiroService.listarUltimos(usuarioId, LIMITE_ULTIMOS),
        lancamentoFinanceiroService.sugerirLancamentoRapido(usuarioId),
        Dinheiro.arredondar(contaService.somarSaldoEmContas(usuarioId)));
  }

  private List<MesDaEvolucao> montarEvolucao(UUID usuarioId, YearMonth competencia) {
    YearMonth primeiro = competencia.minusMonths(MESES_DA_EVOLUCAO);
    Map<YearMonth, TotaisDoMesProjection> porMes = new HashMap<>();
    for (TotaisDoMesProjection totais :
        dashboardFinanceiroRepository.buscarTotaisPorMes(
            usuarioId, primeiro.atDay(1), competencia.atEndOfMonth())) {
      porMes.put(YearMonth.of(totais.getAno(), totais.getMes()), totais);
    }

    List<MesDaEvolucao> meses = new ArrayList<>();
    MesDaEvolucao anterior = null;
    for (YearMonth m = primeiro; !m.isAfter(competencia); m = m.plusMonths(1)) {
      TotaisDoMesProjection totais = porMes.get(m);
      BigDecimal entrou = totais == null ? ZERO : Dinheiro.arredondar(totais.getReceitas());
      BigDecimal saiu = totais == null ? ZERO : Dinheiro.arredondar(totais.getDespesas());
      BigDecimal passiva = totais == null ? ZERO : Dinheiro.arredondar(totais.getReceitaPassiva());
      BigDecimal sobrou = entrou.subtract(saiu);
      MesDaEvolucao atual =
          new MesDaEvolucao(
              m.getYear(),
              m.getMonthValue(),
              entrou,
              saiu,
              sobrou,
              Fracao.de(sobrou, entrou),
              meta(PercentualDeGuardar.TRINTA_POR_CENTO, entrou, sobrou).atingiu(),
              passiva,
              Fracao.de(passiva, saiu),
              anterior == null ? Fracao.ZERO : Fracao.evolucao(entrou, anterior.entrou()),
              anterior == null ? Fracao.ZERO : Fracao.evolucao(saiu, anterior.saiu()));
      meses.add(atual);
      anterior = atual;
    }
    return meses;
  }

  private static MetaDeGuardar meta(
      PercentualDeGuardar meta, BigDecimal entrou, BigDecimal sobrou) {
    BigDecimal percentual = meta.getPercentual();
    BigDecimal valor = Dinheiro.arredondar(entrou.multiply(percentual));
    BigDecimal falta = valor.subtract(sobrou).max(ZERO);
    boolean atingiu = entrou.signum() > 0 && sobrou.compareTo(valor) >= 0;
    return new MetaDeGuardar(percentual, valor, atingiu, falta);
  }

  private static ComparativoMesAnterior comparativo(MesDaEvolucao atual, MesDaEvolucao anterior) {
    return new ComparativoMesAnterior(
        atual.entrou().subtract(anterior.entrou()),
        atual.evolucaoReceitas(),
        atual.saiu().subtract(anterior.saiu()),
        atual.evolucaoDespesas());
  }

  private static OrcamentoDoMes orcamentoDoMes(
      PrevistoRealizadoResponse orcamento, BigDecimal saiu) {
    BigDecimal despesasPrevistas = orcamento.despesas().total().previsto();
    List<CategoriaComPrevisto> grupos =
        orcamento.despesas().grupos().stream()
            .filter(grupo -> grupo.valores().previsto().signum() > 0)
            .map(grupo -> paraCategoria(grupo.categoriaId(), grupo.nome(), null, grupo.valores()))
            .sorted(Comparator.comparing(CategoriaComPrevisto::percentualRealizado).reversed())
            .limit(LIMITE_GRUPOS)
            .toList();
    return new OrcamentoDoMes(
        orcamento.receitas().total().previsto(),
        despesasPrevistas,
        Fracao.de(saiu, despesasPrevistas),
        despesasPrevistas.subtract(saiu),
        grupos);
  }

  private static Independenciometro independenciometro(
      MesDaEvolucao atual, MesDaEvolucao anterior) {
    BigDecimal indice = atual.independenciometro();
    BigDecimal proximoMarco =
        MarcoDoIndependenciometro.proximo(indice)
            .map(MarcoDoIndependenciometro::getPercentual)
            .orElse(null);
    BigDecimal falta =
        proximoMarco == null
            ? ZERO
            : Dinheiro.arredondar(atual.saiu().multiply(proximoMarco))
                .subtract(atual.receitaPassiva())
                .max(ZERO);

    return new Independenciometro(
        indice,
        atual.receitaPassiva(),
        atual.saiu(),
        anterior.independenciometro(),
        indice.subtract(anterior.independenciometro()),
        MarcoDoIndependenciometro.faixa(indice),
        proximoMarco,
        falta);
  }

  private ParaPagar paraPagar(UUID usuarioId, YearMonth competencia) {
    List<LancamentoFinanceiroResponse> pendentes =
        lancamentoFinanceiroService.listarAPagarAte(usuarioId, competencia.atEndOfMonth());
    BigDecimal total =
        pendentes.stream().map(LancamentoFinanceiroResponse::valor).reduce(ZERO, BigDecimal::add);
    return new ParaPagar(
        total,
        pendentes.size(),
        pendentes.subList(0, Math.min(LIMITE_PARA_PAGAR, pendentes.size())));
  }

  private static boolean chegouAoLimiteDoAlerta(Valores valores) {
    return valores.previsto().signum() > 0
        && valores.percentualRealizado().compareTo(PERCENTUAL_DE_ALERTA_DO_PREVISTO) >= 0;
  }

  private static boolean passouDoPrevisto(Valores valores) {
    return valores.previsto().signum() > 0 && valores.desvio().signum() > 0;
  }

  private static List<CategoriaComPrevisto> filtrarSubitens(
      Bloco bloco, Predicate<Valores> criterio) {
    List<CategoriaComPrevisto> encontradas = new ArrayList<>();
    for (Grupo grupo : bloco.grupos()) {
      if (grupo.subitens().isEmpty() && criterio.test(grupo.valores())) {
        encontradas.add(paraCategoria(grupo.categoriaId(), grupo.nome(), null, grupo.valores()));
      }
      for (Item item : grupo.subitens()) {
        if (criterio.test(item.valores())) {
          encontradas.add(
              paraCategoria(item.categoriaId(), item.nome(), grupo.nome(), item.valores()));
        }
      }
    }
    encontradas.sort(Comparator.comparing(CategoriaComPrevisto::percentualRealizado).reversed());
    return encontradas;
  }

  private static CategoriaComPrevisto paraCategoria(
      UUID categoriaId, String nome, String grupo, Valores valores) {
    return new CategoriaComPrevisto(
        categoriaId,
        nome,
        grupo,
        valores.previsto(),
        valores.realizado(),
        valores.desvio(),
        valores.percentualRealizado());
  }

  private static YearMonth competencia(Integer ano, Integer mes, LocalDate hoje) {
    if (ano == null && mes == null) {
      return YearMonth.from(hoje);
    }
    if (ano == null || mes == null) {
      throw new BusinessException("Informe ano e mês juntos, ou nenhum dos dois.");
    }
    return Competencia.de(ano, mes);
  }
}
