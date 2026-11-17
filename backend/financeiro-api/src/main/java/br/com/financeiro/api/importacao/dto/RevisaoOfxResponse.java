package br.com.financeiro.api.importacao.dto;

import java.util.List;

public record RevisaoOfxResponse(List<ItemOfxResponse> itens, String moeda) {}
