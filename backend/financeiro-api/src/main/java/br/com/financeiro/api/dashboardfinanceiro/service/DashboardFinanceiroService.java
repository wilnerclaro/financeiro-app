package br.com.financeiro.api.dashboardfinanceiro.service;

import br.com.financeiro.api.common.util.Competencia;
import br.com.financeiro.api.common.util.Fracao;
import br.com.financeiro.api.dashboardfinanceiro.dto.ResumoCategoriaFinanceiraResponse;
import br.com.financeiro.api.dashboardfinanceiro.dto.ResumoMensalFinanceiroResponse;
import br.com.financeiro.api.dashboardfinanceiro.projection.ResumoCategoriaFinanceiraProjection;
import br.com.financeiro.api.dashboardfinanceiro.projection.ResumoMensalFinanceiroProjection;
import br.com.financeiro.api.dashboardfinanceiro.repository.DashboardFinanceiroRepository;
import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import br.com.financeiro.api.usuario.service.UsuarioService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardFinanceiroService {

  private final DashboardFinanceiroRepository dashboardFinanceiroRepository;
  private final UsuarioService usuarioService;

  @Transactional(readOnly = true)
  public ResumoMensalFinanceiroResponse buscarResumoMensal(
      UUID usuarioId, Integer ano, Integer mes) {
    validarUsuario(usuarioId);
    YearMonth competencia = Competencia.de(ano, mes);

    LocalDate dataInicio = competencia.atDay(1);
    LocalDate dataFim = competencia.atEndOfMonth();

    ResumoMensalFinanceiroProjection resumo =
        dashboardFinanceiroRepository.buscarResumoMensal(usuarioId, dataInicio, dataFim);

    BigDecimal totalReceitas = valorOuZero(resumo.getTotalReceitas());
    BigDecimal totalDespesas = valorOuZero(resumo.getTotalDespesas());
    Long quantidadeLancamentos = resumo.getQuantidadeLancamentos();

    List<ResumoCategoriaFinanceiraResponse> receitasPorCategoria =
        buscarResumoPorCategoria(
            usuarioId, TipoLancamentoFinanceiro.RECEITA, dataInicio, dataFim, totalReceitas);

    List<ResumoCategoriaFinanceiraResponse> despesasPorCategoria =
        buscarResumoPorCategoria(
            usuarioId, TipoLancamentoFinanceiro.DESPESA, dataInicio, dataFim, totalDespesas);

    return new ResumoMensalFinanceiroResponse(
        usuarioId,
        ano,
        mes,
        totalReceitas,
        totalDespesas,
        totalReceitas.subtract(totalDespesas),
        quantidadeLancamentos,
        receitasPorCategoria,
        despesasPorCategoria);
  }

  private List<ResumoCategoriaFinanceiraResponse> buscarResumoPorCategoria(
      UUID usuarioId,
      TipoLancamentoFinanceiro tipo,
      LocalDate dataInicio,
      LocalDate dataFim,
      BigDecimal totalPeriodo) {
    return dashboardFinanceiroRepository
        .buscarResumoPorCategoria(usuarioId, tipo, dataInicio, dataFim)
        .stream()
        .map(item -> paraResponse(item, totalPeriodo))
        .toList();
  }

  private ResumoCategoriaFinanceiraResponse paraResponse(
      ResumoCategoriaFinanceiraProjection projection, BigDecimal totalPeriodo) {
    BigDecimal total = valorOuZero(projection.getTotal());

    return new ResumoCategoriaFinanceiraResponse(
        projection.getCategoriaId(),
        projection.getCategoriaNome(),
        total,
        Fracao.de(total, totalPeriodo),
        projection.getQuantidadeLancamentos());
  }

  private BigDecimal valorOuZero(BigDecimal valor) {
    return valor == null ? BigDecimal.ZERO : valor;
  }

  private void validarUsuario(UUID usuarioId) {
    usuarioService.validarExistencia(usuarioId);
  }
}
