package br.com.financeiro.api.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(
    @Schema(description = "Vai no header Authorization: Bearer <accessToken>.") String accessToken,
    @Schema(description = "Usado só em /api/auth/refresh para obter um novo par de tokens.")
        String refreshToken,
    @Schema(example = "Bearer") String tipo,
    @Schema(description = "Validade do accessToken, em segundos.", example = "900") long expiraEm,
    UsuarioResponse usuario) {}
