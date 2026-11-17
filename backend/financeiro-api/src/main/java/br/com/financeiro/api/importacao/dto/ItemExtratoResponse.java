package br.com.financeiro.api.importacao.dto;

import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemExtratoResponse(
    String identificador,
    LocalDate data,
    String descricao,
    BigDecimal valor,
    TipoLancamentoFinanceiro tipo,
    @Schema(description = "Já entrou por uma importação anterior; não entra de novo.")
        boolean jaImportado,
    @Schema(
            description =
                "Há na conta um lançamento com a mesma data, tipo e valor. A tela deve vir"
                    + " desmarcada para o usuário decidir.")
        boolean possivelDuplicado,
    boolean importavel,
    String motivo) {}
