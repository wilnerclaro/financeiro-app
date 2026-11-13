package br.com.financeiro.api.lancamentofinanceiro.repository;

import br.com.financeiro.api.lancamentofinanceiro.entity.LancamentoFinanceiro;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LancamentoFinanceiroRepository
    extends JpaRepository<LancamentoFinanceiro, UUID>,
        JpaSpecificationExecutor<LancamentoFinanceiro> {

  @Query(
      """
            SELECT l
            FROM LancamentoFinanceiro l
            WHERE l.id = :id
              AND l.usuario.id = :usuarioId
            """)
  Optional<LancamentoFinanceiro> buscarPorIdEUsuarioId(
      @Param("id") UUID id, @Param("usuarioId") UUID usuarioId);
}
