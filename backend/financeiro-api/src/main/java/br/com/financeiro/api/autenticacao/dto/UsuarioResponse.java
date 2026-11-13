package br.com.financeiro.api.autenticacao.dto;

import java.util.UUID;

public record UsuarioResponse(UUID id, String nome, String email) {}
