package br.com.financeiro.api.dashboardfinanceiro.repository;

import br.com.financeiro.api.dashboardfinanceiro.projection.ResumoCategoriaFinanceiraProjection;
import br.com.financeiro.api.dashboardfinanceiro.projection.ResumoMensalFinanceiroProjection;
import br.com.financeiro.api.lancamentofinanceiro.entity.LancamentoFinanceiro;
import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface DashboardFinanceiroRepository extends Repository<LancamentoFinanceiro, UUID> {

  @Query(
      value =
          """
            SELECT
                COALESCE(SUM(CASE WHEN tipo = 'RECEITA' THEN valor ELSE 0 END), 0) AS totalReceitas,
                COALESCE(SUM(CASE WHEN tipo = 'DESPESA' THEN valor ELSE 0 END), 0) AS totalDespesas,
                COUNT(*) AS quantidadeLancamentos
            FROM lancamentos_financeiros
            WHERE usuario_id = :usuarioId
              AND status = 'PAGO'
              AND data_competencia >= :dataInicio
              AND data_competencia <= :dataFim
            """,
      nativeQuery = true)
  ResumoMensalFinanceiroProjection buscarResumoMensal(
      @Param("usuarioId") UUID usuarioId,
      @Param("dataInicio") LocalDate dataInicio,
      @Param("dataFim") LocalDate dataFim);

  @Query(
      """
            SELECT
                c.id AS categoriaId,
                c.nome AS categoriaNome,
                COALESCE(SUM(l.valor), 0) AS total,
                COUNT(l.id) AS quantidadeLancamentos
            FROM LancamentoFinanceiro l
            JOIN l.categoria c
            WHERE l.usuario.id = :usuarioId
              AND l.status = 'PAGO'
              AND l.tipo = :tipo
              AND l.dataCompetencia >= :dataInicio
              AND l.dataCompetencia <= :dataFim
            GROUP BY c.id, c.nome
            ORDER BY COALESCE(SUM(l.valor), 0) DESC, c.nome ASC
            """)
  List<ResumoCategoriaFinanceiraProjection> buscarResumoPorCategoria(
      @Param("usuarioId") UUID usuarioId,
      @Param("tipo") TipoLancamentoFinanceiro tipo,
      @Param("dataInicio") LocalDate dataInicio,
      @Param("dataFim") LocalDate dataFim);
}
