package br.com.financeiro.api.importacao.service;

import br.com.financeiro.api.common.util.NormalizadorTexto;
import br.com.financeiro.api.importacao.dto.TransacaoLida;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeitorPdfItauService implements LeitorDeExtratoPdf {
  private static final Pattern LANCAMENTO =
      Pattern.compile(
          "^("
              + ConversorDeExtratoService.DATA
              + ")\\s+(.+?)\\s+("
              + ConversorDeExtratoService.VALOR
              + ")$");
  private static final Pattern SALDO_DO_DIA =
      Pattern.compile(
          "^" + ConversorDeExtratoService.DATA + "\\s+" + ConversorDeExtratoService.VALOR + "$");
  private static final String CABECALHO = "data lancamentos descricao valor (r$) saldo (r$)";
  private static final String TIPO_QUEBRADO = "PAGAMENTO DE PIX QR";
  private static final String FIM_DO_TIPO_QUEBRADO = "CODE";
  private final ConversorDeExtratoService conversor;

  @Override
  public String banco() {
    return "Itaú";
  }

  @Override
  public boolean reconhece(List<String> linhas) {
    return conversor.contemLinha(linhas, "Extrato conta corrente")
        && linhas.stream()
            .map(NormalizadorTexto::normalizarParaComparacao)
            .anyMatch(linha -> linha.contains("itau"))
        && linhas.stream()
            .map(NormalizadorTexto::normalizarParaComparacao)
            .anyMatch(CABECALHO::equals);
  }

  @Override
  public List<TransacaoLida> ler(List<String> linhas) {
    List<TransacaoLida> transacoes = new ArrayList<>();
    Lancamento atual = null;
    boolean emLancamentos = false;
    for (String linha : linhas) {
      String normalizada = NormalizadorTexto.normalizarParaComparacao(linha);
      if (normalizada.equals("lancamentos")) {
        emLancamentos = true;
      } else if (normalizada.startsWith("posicao consolidada")) {
        break;
      } else if (emLancamentos && !CABECALHO.equals(normalizada)) {
        Matcher lancamento = LANCAMENTO.matcher(linha);
        if (SALDO_DO_DIA.matcher(linha).matches()) {
          atual = fechar(atual, transacoes);
        } else if (lancamento.matches()) {
          fechar(atual, transacoes);
          atual = new Lancamento(lancamento.group(1), lancamento.group(2), lancamento.group(3));
        } else if (atual != null) {
          atual.continuar(linha);
        }
      }
    }
    fechar(atual, transacoes);
    return transacoes;
  }

  private Lancamento fechar(Lancamento lancamento, List<TransacaoLida> transacoes) {
    if (lancamento != null && !conversor.ehSaldo(lancamento.descricao())) {
      transacoes.add(
          conversor.transacao(
              conversor.data(lancamento.data),
              lancamento.descricao(),
              conversor.valor(lancamento.valor)));
    }
    return null;
  }

  private static final class Lancamento {
    private final String data;
    private final String valor;
    private final StringBuilder texto;
    private int continuacoes;

    private Lancamento(String data, String texto, String valor) {
      this.data = data;
      this.valor = valor;
      this.texto = new StringBuilder(texto);
    }

    private void continuar(String linha) {
      String complemento = linha;
      boolean primeira = continuacoes++ == 0;
      if (primeira
          && texto.indexOf(TIPO_QUEBRADO + " ") == 0
          && (complemento.equals(FIM_DO_TIPO_QUEBRADO)
              || complemento.startsWith(FIM_DO_TIPO_QUEBRADO + " "))) {
        texto.insert(TIPO_QUEBRADO.length(), " " + FIM_DO_TIPO_QUEBRADO);
        complemento = complemento.substring(FIM_DO_TIPO_QUEBRADO.length());
      }
      texto.append(' ').append(complemento);
    }

    private String descricao() {
      return texto.toString();
    }
  }
}
