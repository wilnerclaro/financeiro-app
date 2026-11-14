package br.com.financeiro.api.conta.dto;

import br.com.financeiro.api.conta.enums.TipoConta;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record ResumoContasResponse(
    @Schema(description = "Soma do saldo atual das contas ativas; cartão entra negativo.")
        BigDecimal patrimonio,
    @Schema(description = "Soma do saldo das contas ativas por tipo, do maior para o menor.")
        List<TotalPorTipo> porTipo) {

  public record TotalPorTipo(TipoConta tipo, BigDecimal total, long quantidadeDeContas) {}
}
