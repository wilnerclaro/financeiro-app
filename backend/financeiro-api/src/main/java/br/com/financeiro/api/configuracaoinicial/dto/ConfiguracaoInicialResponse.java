package br.com.financeiro.api.configuracaoinicial.dto;

import br.com.financeiro.api.configuracaoinicial.enums.EtapaConfiguracao;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record ConfiguracaoInicialResponse(
    @Schema(description = "Renda passiva mensal desejada, separada dos valores realizados.")
        BigDecimal rendaPassivaDesejada,
    EtapaConfiguracao etapa,
    boolean pausado,
    int ano,
    int mes) {}
