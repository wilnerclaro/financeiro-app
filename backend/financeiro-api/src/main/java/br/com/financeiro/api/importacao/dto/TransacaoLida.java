package br.com.financeiro.api.importacao.dto;

import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoLida(
    String identificador,
    LocalDate data,
    String descricao,
    BigDecimal valor,
    TipoLancamentoFinanceiro tipo,
    String motivo) {

  public boolean importavel() {
    return motivo == null;
  }

  public TransacaoLida comIdentificador(String novoIdentificador) {
    return new TransacaoLida(novoIdentificador, data, descricao, valor, tipo, motivo);
  }
}
