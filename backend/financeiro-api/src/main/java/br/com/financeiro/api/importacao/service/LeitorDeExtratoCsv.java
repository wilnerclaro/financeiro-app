package br.com.financeiro.api.importacao.service;

import br.com.financeiro.api.importacao.dto.TransacaoLida;
import java.util.List;

public interface LeitorDeExtratoCsv {
  String banco();

  boolean reconhece(List<List<String>> linhas);

  List<TransacaoLida> ler(List<List<String>> linhas);
}
