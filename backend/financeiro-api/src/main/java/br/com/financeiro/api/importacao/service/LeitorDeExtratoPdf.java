package br.com.financeiro.api.importacao.service;

import br.com.financeiro.api.importacao.dto.TransacaoLida;
import java.util.List;

public interface LeitorDeExtratoPdf {
  String banco();

  boolean reconhece(List<String> linhas);

  List<TransacaoLida> ler(List<String> linhas);
}
