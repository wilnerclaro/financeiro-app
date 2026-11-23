package br.com.financeiro.api.autenticacao.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "O refresh token é obrigatório.") String refreshToken) {}
