package br.com.financeiro.api.importacao.dto;

import br.com.financeiro.api.importacao.enums.FormatoExtrato;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record RevisaoExtratoResponse(
    FormatoExtrato formato,
    @Schema(description = "Banco reconhecido no arquivo; nulo no OFX.") String banco,
    List<ItemExtratoResponse> itens,
    String moeda) {}
