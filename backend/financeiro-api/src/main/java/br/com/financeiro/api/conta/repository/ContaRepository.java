package br.com.financeiro.api.conta.repository;

import br.com.financeiro.api.conta.entity.Conta;
import br.com.financeiro.api.conta.enums.TipoConta;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContaRepository extends JpaRepository<Conta, UUID> {

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

  /** Soma dos lançamentos pagos da conta: receitas somam e despesas subtraem. */
  @Query(
      value =
          """
            SELECT COALESCE(SUM(
                CASE
                    WHEN tipo = 'RECEITA' THEN valor
                    WHEN tipo = 'DESPESA' THEN -valor
                    ELSE 0
                END
            ), 0)
            FROM lancamentos_financeiros
            WHERE conta_id = :contaId
              AND usuario_id = :usuarioId
              AND status = 'PAGO'
            """,
      nativeQuery = true)
  BigDecimal calcularSaldoMovimentado(
      @Param("contaId") UUID contaId, @Param("usuarioId") UUID usuarioId);
}
