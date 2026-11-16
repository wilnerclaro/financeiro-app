package br.com.financeiro.api.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ConfiguracaoAutenticacaoResponse(
    @Schema(
            description =
                "Client ID OAuth do Google para o botão Entrar com Google. Nulo quando o login com"
                    + " Google não está configurado; nesse caso o front esconde o botão.")
        String googleClientId) {}
