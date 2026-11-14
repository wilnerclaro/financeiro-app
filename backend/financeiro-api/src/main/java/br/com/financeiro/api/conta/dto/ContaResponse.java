package br.com.financeiro.api.conta.dto;

import br.com.financeiro.api.conta.enums.TipoConta;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ContaResponse(
    UUID id,
    UUID usuarioId,
    String nome,
    TipoConta tipo,
    BigDecimal saldoInicial,
    BigDecimal saldoAtual,
    @Schema(description = "Saldo atual − saldo inicial.") BigDecimal variacaoDesdeSaldoInicial,
    Boolean ativa,
    Integer diaFechamento,
    Integer diaVencimento,
    OffsetDateTime criadoEm,
    OffsetDateTime atualizadoEm) {}
