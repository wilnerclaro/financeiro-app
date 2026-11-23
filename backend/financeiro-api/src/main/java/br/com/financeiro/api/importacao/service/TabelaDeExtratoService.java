package br.com.financeiro.api.importacao.service;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.util.NormalizadorTexto;
import br.com.financeiro.api.importacao.dto.TransacaoLida;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TabelaDeExtratoService {
  private final ConversorDeExtratoService conversor;

  public int posicaoDoCabecalho(List<List<String>> linhas, List<String> cabecalho) {
    for (int posicao = 0; posicao < linhas.size(); posicao++) {
      List<String> colunas =
          linhas.get(posicao).stream().map(NormalizadorTexto::normalizarParaComparacao).toList();
      if (colunas.size() >= cabecalho.size()
          && colunas.subList(0, cabecalho.size()).equals(cabecalho)) {
        return posicao;
      }
    }
    return -1;
  }

  public List<TransacaoLida> ler(
      List<List<String>> linhas,
      List<String> cabecalho,
      Function<List<String>, String> descricao,
      int colunaDoValor) {
    List<TransacaoLida> transacoes = new ArrayList<>();
    for (List<String> linha :
        linhas.subList(posicaoDoCabecalho(linhas, cabecalho) + 1, linhas.size())) {
      if (linha.isEmpty() || !conversor.ehData(linha.getFirst())) {
        continue;
      }
      if (linha.size() <= colunaDoValor) {
        throw new BusinessException("O CSV tem uma linha de lançamento incompleta.");
      }
      String texto = descricao.apply(linha);
      if (!conversor.ehSaldo(texto)) {
        transacoes.add(
            conversor.transacao(
                conversor.data(linha.getFirst()),
                texto,
                conversor.valor(linha.get(colunaDoValor))));
      }
    }
    return transacoes;
  }
}
