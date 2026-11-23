package br.com.financeiro.api.orcamento.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DefinirOrcamentoRequest(
    @NotEmpty(message = "Informe ao menos uma categoria.") List<@Valid Item> itens) {

  public record Item(
      @NotNull(message = "A categoria é obrigatória.") UUID categoriaId,
      @NotNull(message = "O valor previsto é obrigatório.")
          @DecimalMin(value = "0.00", message = "O valor previsto não pode ser negativo.")
          @Digits(
              integer = 17,
              fraction = 2,
              message =
                  "O valor previsto deve ter no máximo 17 dígitos inteiros e 2 casas decimais.")
          BigDecimal valorPrevisto) {}
}
