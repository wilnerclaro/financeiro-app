package br.com.financeiro.api.transferencia.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransferenciaResponse(
    UUID id,
    UUID contaOrigemId,
    String nomeContaOrigem,
    UUID contaDestinoId,
    String nomeContaDestino,
    BigDecimal valor,
    LocalDate dataTransferencia,
    String descricao,
    Boolean cancelada,
    OffsetDateTime criadoEm,
    OffsetDateTime atualizadoEm) {}
