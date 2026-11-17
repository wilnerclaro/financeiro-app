package br.com.financeiro.api.importacao.service;

import br.com.financeiro.api.importacao.dto.TransacaoLida;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeitorCsvSantanderService implements LeitorDeExtratoCsv {
  private static final List<String> CABECALHO =
      List.of("data", "descricao", "detalhe", "valor", "saldo");
  private final TabelaDeExtratoService tabela;

  @Override
  public String banco() {
    return "Santander";
  }

  @Override
  public boolean reconhece(List<List<String>> linhas) {
    return tabela.posicaoDoCabecalho(linhas, CABECALHO) >= 0;
  }

  @Override
  public List<TransacaoLida> ler(List<List<String>> linhas) {
    return tabela.ler(linhas, CABECALHO, linha -> juntar(linha.get(1), linha.get(2)), 3);
  }

  private String juntar(String descricao, String detalhe) {
    return detalhe.isBlank() ? descricao : descricao.strip() + " - " + detalhe.strip();
  }
}
