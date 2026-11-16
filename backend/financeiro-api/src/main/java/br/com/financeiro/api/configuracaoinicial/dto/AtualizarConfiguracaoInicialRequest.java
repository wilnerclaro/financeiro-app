package br.com.financeiro.api.configuracaoinicial.dto;

import br.com.financeiro.api.configuracaoinicial.enums.EtapaConfiguracao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AtualizarConfiguracaoInicialRequest(
    @Schema(description = "Objetivo de renda passiva mensal; não altera o Independenciômetro.")
        @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 17, fraction = 2)
        BigDecimal rendaPassivaDesejada,
    @NotNull EtapaConfiguracao etapa,
    @NotNull Boolean pausado,
    @NotNull Integer ano,
    @NotNull Integer mes) {}
