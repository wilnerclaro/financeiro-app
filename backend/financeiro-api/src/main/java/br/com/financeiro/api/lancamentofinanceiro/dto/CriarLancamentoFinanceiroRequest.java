package br.com.financeiro.api.lancamentofinanceiro.dto;

import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CriarLancamentoFinanceiroRequest(
    @NotNull(message = "O usuário é obrigatório.") UUID usuarioId,
    @NotNull(message = "A conta é obrigatória.") UUID contaId,
    @NotNull(message = "A categoria é obrigatória.") UUID categoriaId,
    @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 180, message = "A descrição deve ter no máximo 180 caracteres.")
        String descricao,
    @NotNull(message = "O tipo do lançamento é obrigatório.") TipoLancamentoFinanceiro tipo,
    @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O valor deve ter no máximo 17 dígitos inteiros e 2 casas decimais.")
        BigDecimal valor,
    @NotNull(message = "A data de competência é obrigatória.") LocalDate dataCompetencia,
    LocalDate dataVencimento,
    LocalDate dataPagamento,
    @Size(max = 2000, message = "A observação deve ter no máximo 2000 caracteres.")
        String observacao) {}
