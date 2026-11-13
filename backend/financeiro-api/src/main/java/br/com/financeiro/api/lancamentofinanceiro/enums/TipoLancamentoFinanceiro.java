package br.com.financeiro.api.lancamentofinanceiro.enums;

import br.com.financeiro.api.categoria.enums.TipoCategoria;

public enum TipoLancamentoFinanceiro {
  DESPESA,
  RECEITA;

  /** Um lançamento só pode usar uma categoria do mesmo tipo (receita com receita, etc.). */
  public boolean aceitaCategoria(TipoCategoria tipoCategoria) {
    return switch (this) {
      case DESPESA -> tipoCategoria == TipoCategoria.DESPESA;
      case RECEITA -> tipoCategoria == TipoCategoria.RECEITA;
    };
  }
}
