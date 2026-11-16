package br.com.financeiro.api.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record EntrarComGoogleRequest(
    @Schema(description = "ID token (credential) devolvido pelo Google Identity Services.")
        @NotBlank(message = "O token do Google é obrigatório.")
        String idToken) {}
