package br.com.financeiro.api.lancamentofinanceiro.repository;

import br.com.financeiro.api.lancamentofinanceiro.entity.LancamentoFinanceiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface LancamentoFinanceiroRepository extends JpaRepository<LancamentoFinanceiro, UUID>,
        JpaSpecificationExecutor<LancamentoFinanceiro> {

    @Query("""
            SELECT l
            FROM LancamentoFinanceiro l
            WHERE l.id = :id
              AND l.usuario.id = :usuarioId
            """)
    Optional<LancamentoFinanceiro> buscarPorIdEUsuarioId(
            @Param("id") UUID id,
            @Param("usuarioId") UUID usuarioId
    );

    @Query(value = """
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
            """, nativeQuery = true)
    BigDecimal calcularSaldoMovimentadoPorConta(
            @Param("contaId") UUID contaId,
            @Param("usuarioId") UUID usuarioId
    );
}
