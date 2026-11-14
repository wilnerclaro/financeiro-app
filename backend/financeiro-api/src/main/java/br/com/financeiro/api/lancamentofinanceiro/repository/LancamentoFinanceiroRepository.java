package br.com.financeiro.api.lancamentofinanceiro.repository;

import br.com.financeiro.api.lancamentofinanceiro.entity.LancamentoFinanceiro;
import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LancamentoFinanceiroRepository
    extends JpaRepository<LancamentoFinanceiro, UUID>,
        JpaSpecificationExecutor<LancamentoFinanceiro> {

  @Override
  @EntityGraph(attributePaths = {"conta", "categoria"})
  Page<LancamentoFinanceiro> findAll(Specification<LancamentoFinanceiro> spec, Pageable pageable);

  @Query(
      """
            SELECT l
            FROM LancamentoFinanceiro l
            WHERE l.id = :id
              AND l.usuario.id = :usuarioId
            """)
  Optional<LancamentoFinanceiro> buscarPorIdEUsuarioId(
      @Param("id") UUID id, @Param("usuarioId") UUID usuarioId);

  @EntityGraph(attributePaths = {"conta", "categoria"})
  @Query(
      """
            SELECT l
            FROM LancamentoFinanceiro l
            WHERE l.usuario.id = :usuarioId
              AND l.conta.id = :contaId
              AND l.status <> br.com.financeiro.api.lancamentofinanceiro.enums.StatusLancamentoFinanceiro.CANCELADO
              AND l.dataCompetencia BETWEEN :inicio AND :fim
            ORDER BY l.dataCompra, l.criadoEm
            """)
  List<LancamentoFinanceiro> buscarDaFatura(
      @Param("usuarioId") UUID usuarioId,
      @Param("contaId") UUID contaId,
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim);

  interface CategoriaMaisUsada {
    UUID getCategoriaId();

    String getNome();

    Long getQuantidade();
  }

  @EntityGraph(attributePaths = {"conta", "categoria"})
  @Query(
      """
            SELECT l
            FROM LancamentoFinanceiro l
            WHERE l.usuario.id = :usuarioId
              AND l.status = br.com.financeiro.api.lancamentofinanceiro.enums.StatusLancamentoFinanceiro.PENDENTE
              AND l.dataVencimento <= :ate
            ORDER BY l.dataVencimento, l.criadoEm
            """)
  List<LancamentoFinanceiro> buscarAPagarAte(
      @Param("usuarioId") UUID usuarioId, @Param("ate") LocalDate ate);

  @EntityGraph(attributePaths = {"conta", "categoria"})
  @Query(
      """
            SELECT l
            FROM LancamentoFinanceiro l
            WHERE l.usuario.id = :usuarioId
              AND l.status <> br.com.financeiro.api.lancamentofinanceiro.enums.StatusLancamentoFinanceiro.CANCELADO
            ORDER BY COALESCE(l.dataPagamento, l.dataCompetencia) DESC, l.criadoEm DESC
            """)
  List<LancamentoFinanceiro> buscarUltimos(@Param("usuarioId") UUID usuarioId, Pageable pageable);

  @EntityGraph(attributePaths = "conta")
  @Query(
      """
            SELECT l
            FROM LancamentoFinanceiro l
            WHERE l.usuario.id = :usuarioId
              AND l.conta.ativa = TRUE
            ORDER BY l.criadoEm DESC
            """)
  List<LancamentoFinanceiro> buscarMaisRecentesEmContaAtiva(
      @Param("usuarioId") UUID usuarioId, Pageable pageable);

  @Query(
      """
            SELECT c.id AS categoriaId, c.nome AS nome, COUNT(l) AS quantidade
            FROM LancamentoFinanceiro l
            JOIN l.categoria c
            WHERE l.usuario.id = :usuarioId
              AND l.tipo = :tipo
              AND c.ativa = TRUE
              AND l.criadoEm >= :desde
            GROUP BY c.id, c.nome
            ORDER BY COUNT(l) DESC, c.nome
            """)
  List<CategoriaMaisUsada> buscarCategoriasMaisUsadas(
      @Param("usuarioId") UUID usuarioId,
      @Param("tipo") TipoLancamentoFinanceiro tipo,
      @Param("desde") OffsetDateTime desde,
      Pageable pageable);
}
