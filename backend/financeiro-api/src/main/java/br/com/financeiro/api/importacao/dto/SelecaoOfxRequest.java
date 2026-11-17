package br.com.financeiro.api.importacao.dto;

import java.util.UUID;

public record SelecaoOfxRequest(String identificador, UUID categoriaId) {}
