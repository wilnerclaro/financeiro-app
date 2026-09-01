package br.com.financeiro.api.conta.dto;

import br.com.financeiro.api.conta.enums.TipoConta;
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
    Boolean ativa,
    OffsetDateTime criadoEm,
    OffsetDateTime atualizadoEm) {}
