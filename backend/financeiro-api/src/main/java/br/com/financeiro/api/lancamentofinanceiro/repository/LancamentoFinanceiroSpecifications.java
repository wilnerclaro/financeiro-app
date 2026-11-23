package br.com.financeiro.api.lancamentofinanceiro.repository;

import br.com.financeiro.api.lancamentofinanceiro.entity.LancamentoFinanceiro;
import br.com.financeiro.api.lancamentofinanceiro.enums.StatusLancamentoFinanceiro;
import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class LancamentoFinanceiroSpecifications {

  private LancamentoFinanceiroSpecifications() {}

  public static Specification<LancamentoFinanceiro> doUsuario(UUID usuarioId) {
    return (root, query, cb) -> cb.equal(root.get("usuario").get("id"), usuarioId);
  }

  public static Specification<LancamentoFinanceiro> doTipo(TipoLancamentoFinanceiro tipo) {
    return (root, query, cb) -> tipo == null ? null : cb.equal(root.get("tipo"), tipo);
  }

  public static Specification<LancamentoFinanceiro> daConta(UUID contaId) {
    return (root, query, cb) ->
        contaId == null ? null : cb.equal(root.get("conta").get("id"), contaId);
  }

  public static Specification<LancamentoFinanceiro> daCategoria(UUID categoriaId) {
    return (root, query, cb) ->
        categoriaId == null ? null : cb.equal(root.get("categoria").get("id"), categoriaId);
  }

  public static Specification<LancamentoFinanceiro> competenciaEntre(
      LocalDate dataInicio, LocalDate dataFim) {
    return (root, query, cb) -> {
      if (dataInicio != null && dataFim != null) {
        return cb.between(root.get("dataCompetencia"), dataInicio, dataFim);
      }
      if (dataInicio != null) {
        return cb.greaterThanOrEqualTo(root.get("dataCompetencia"), dataInicio);
      }
      if (dataFim != null) {
        return cb.lessThanOrEqualTo(root.get("dataCompetencia"), dataFim);
      }
      return null;
    };
  }

  public static Specification<LancamentoFinanceiro> comStatus(
      StatusLancamentoFinanceiro status, LocalDate hoje) {
    return (root, query, cb) -> {
      if (status == null) {
        return null;
      }
      var vencimento = root.<LocalDate>get("dataVencimento");
      var pendente = cb.equal(root.get("status"), StatusLancamentoFinanceiro.PENDENTE);

      return switch (status) {
        case ATRASADO -> cb.and(pendente, cb.lessThan(vencimento, hoje));
        case PENDENTE ->
            cb.and(
                pendente, cb.or(cb.isNull(vencimento), cb.greaterThanOrEqualTo(vencimento, hoje)));
        case PAGO, CANCELADO -> cb.equal(root.get("status"), status);
      };
    };
  }
}
