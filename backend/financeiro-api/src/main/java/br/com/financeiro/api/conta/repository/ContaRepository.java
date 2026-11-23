package br.com.financeiro.api.conta.repository;

import br.com.financeiro.api.conta.entity.Conta;
import br.com.financeiro.api.conta.enums.TipoConta;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContaRepository extends JpaRepository<Conta, UUID> {

  interface TotalPorTipo {
    TipoConta getTipo();

    BigDecimal getTotal();

    Long getQuantidade();
  }

  @Query(
      """
            SELECT c
            FROM Conta c
            WHERE c.usuario.id = :usuarioId
              AND (:tipo IS NULL OR c.tipo = :tipo)
              AND (:ativa IS NULL OR c.ativa = :ativa)
            """)
  Page<Conta> buscarPorFiltros(
      @Param("usuarioId") UUID usuarioId,
      @Param("tipo") TipoConta tipo,
      @Param("ativa") Boolean ativa,
      Pageable pageable);

  @Query(
      """
            SELECT c
            FROM Conta c
            WHERE c.id = :id
              AND c.usuario.id = :usuarioId
            """)
  Optional<Conta> buscarPorIdEUsuarioId(@Param("id") UUID id, @Param("usuarioId") UUID usuarioId);

  @Query(
      """
            SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END
            FROM Conta c
            WHERE c.usuario.id = :usuarioId
              AND LOWER(c.nome) = LOWER(:nome)
              AND (:contaIdIgnorada IS NULL OR c.id <> :contaIdIgnorada)
            """)
  boolean existeContaDuplicada(
      @Param("usuarioId") UUID usuarioId,
      @Param("nome") String nome,
      @Param("contaIdIgnorada") UUID contaIdIgnorada);

  @Query(
      value =
          """
            SELECT
              (SELECT COALESCE(SUM(CASE WHEN tipo = 'RECEITA' THEN valor ELSE -valor END), 0)
                 FROM lancamentos_financeiros
                WHERE conta_id = :contaId AND usuario_id = :usuarioId AND status = 'PAGO')
            + (SELECT COALESCE(SUM(valor), 0)
                 FROM transferencias
                WHERE conta_destino_id = :contaId AND usuario_id = :usuarioId AND NOT cancelada)
            - (SELECT COALESCE(SUM(valor), 0)
                 FROM transferencias
                WHERE conta_origem_id = :contaId AND usuario_id = :usuarioId AND NOT cancelada)
            """,
      nativeQuery = true)
  BigDecimal calcularSaldoMovimentado(
      @Param("contaId") UUID contaId, @Param("usuarioId") UUID usuarioId);

  @Query(
      """
            SELECT COALESCE(SUM(c.saldoAtual), 0)
            FROM Conta c
            WHERE c.usuario.id = :usuarioId
              AND c.ativa = TRUE
              AND c.tipo <> br.com.financeiro.api.conta.enums.TipoConta.CARTAO_CREDITO
            """)
  BigDecimal somarSaldoEmContas(@Param("usuarioId") UUID usuarioId);

  @Query(
      """
            SELECT c.tipo AS tipo, COALESCE(SUM(c.saldoAtual), 0) AS total, COUNT(c) AS quantidade
            FROM Conta c
            WHERE c.usuario.id = :usuarioId
              AND c.ativa = TRUE
            GROUP BY c.tipo
            ORDER BY COALESCE(SUM(c.saldoAtual), 0) DESC
            """)
  List<TotalPorTipo> somarPorTipo(@Param("usuarioId") UUID usuarioId);

  @Query(
      value =
          """
            SELECT EXISTS (SELECT 1 FROM lancamentos_financeiros WHERE conta_id = :contaId)
            """,
      nativeQuery = true)
  boolean possuiLancamentos(@Param("contaId") UUID contaId);
}
