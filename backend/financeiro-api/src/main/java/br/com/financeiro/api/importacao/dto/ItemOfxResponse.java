package br.com.financeiro.api.importacao.dto;

import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemOfxResponse(
    String identificador,
    LocalDate data,
    String descricao,
    BigDecimal valor,
    TipoLancamentoFinanceiro tipo,
    boolean jaImportado,
    boolean importavel,
    String motivo) {}
