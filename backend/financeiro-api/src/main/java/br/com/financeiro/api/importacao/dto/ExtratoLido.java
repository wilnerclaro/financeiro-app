package br.com.financeiro.api.importacao.dto;

import br.com.financeiro.api.importacao.enums.FormatoExtrato;
import java.util.List;

public record ExtratoLido(FormatoExtrato formato, String banco, List<TransacaoLida> transacoes) {}
