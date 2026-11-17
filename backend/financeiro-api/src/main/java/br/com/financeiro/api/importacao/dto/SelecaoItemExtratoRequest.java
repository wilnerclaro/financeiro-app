package br.com.financeiro.api.importacao.dto;

import java.util.UUID;

public record SelecaoItemExtratoRequest(String identificador, UUID categoriaId) {}
