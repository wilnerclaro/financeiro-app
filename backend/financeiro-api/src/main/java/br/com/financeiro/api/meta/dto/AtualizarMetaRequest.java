package br.com.financeiro.api.meta.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AtualizarMetaRequest(
    @Schema(example = "Reserva de emergência")
        @NotBlank(message = "O nome da meta é obrigatório.")
        @Size(max = 120, message = "O nome da meta deve ter no máximo 120 caracteres.")
        String nome,
    @Schema(description = "NECESSIDADE: quanto juntar.", example = "30000.00")
        @NotNull(message = "O valor necessário é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor necessário deve ser maior que zero.")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O valor deve ter no máximo 2 casas decimais.")
        BigDecimal valorNecessario,
    @Schema(description = "INVESTIMENTO: quanto já está guardado. Padrão 0.", example = "5000.00")
        @DecimalMin(value = "0.00", message = "O investimento inicial não pode ser negativo.")
        @Digits(
            integer = 17,
            fraction = 2,
            message = "O valor deve ter no máximo 2 casas decimais.")
        BigDecimal investimentoInicial) {}
