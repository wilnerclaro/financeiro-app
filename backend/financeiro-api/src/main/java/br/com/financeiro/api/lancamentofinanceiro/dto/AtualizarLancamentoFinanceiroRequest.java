package br.com.financeiro.api.lancamentofinanceiro.dto;

import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AtualizarLancamentoFinanceiroRequest(

        @NotNull(message = "O usuario e obrigatorio.")
        UUID usuarioId,

        @NotNull(message = "A conta e obrigatoria.")
        UUID contaId,

        @NotNull(message = "A categoria e obrigatoria.")
        UUID categoriaId,

        @NotBlank(message = "A descricao e obrigatoria.")
        @Size(max = 180, message = "A descricao deve ter no maximo 180 caracteres.")
        String descricao,

        @NotNull(message = "O tipo do lancamento e obrigatorio.")
        TipoLancamentoFinanceiro tipo,

        @NotNull(message = "O valor e obrigatorio.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        @Digits(integer = 17, fraction = 2, message = "O valor deve ter no maximo 17 digitos inteiros e 2 casas decimais.")
        BigDecimal valor,

        @NotNull(message = "A data de competencia e obrigatoria.")
        LocalDate dataCompetencia,

        LocalDate dataVencimento,

        LocalDate dataPagamento,

        @Size(max = 2000, message = "A observacao deve ter no maximo 2000 caracteres.")
        String observacao
) {
}
