package br.com.financeiro.api.lancamentofinanceiro.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

public record LancamentoRapidoResponse(
    @Schema(description = "Conta do último lançamento, se ainda estiver ativa.") UUID contaId,
    String nomeConta,
    @Schema(description = "Categorias de despesa mais usadas nos últimos 90 dias, até 6.")
        List<Categoria> categoriasMaisUsadas) {

  public record Categoria(UUID categoriaId, String nome, long quantidade) {}
}
