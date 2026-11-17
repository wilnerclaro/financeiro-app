package br.com.financeiro.api.importacao.service;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.util.NormalizadorTexto;
import br.com.financeiro.api.importacao.dto.TransacaoLida;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeitorPdfInterService implements LeitorDeExtratoPdf {
  private static final Pattern DIA =
      Pattern.compile("^(\\d{1,2}) de (\\p{L}+) de (\\d{4})\\b.*saldo do dia.*$");
  private static final Pattern MOVIMENTO =
      Pattern.compile(
          "^(.+?)\\s+("
              + ConversorDeExtratoService.VALOR
              + ")\\s+"
              + ConversorDeExtratoService.VALOR
              + "$");
  private static final List<String> MESES =
      List.of(
          "janeiro",
          "fevereiro",
          "marco",
          "abril",
          "maio",
          "junho",
          "julho",
          "agosto",
          "setembro",
          "outubro",
          "novembro",
          "dezembro");
  private final ConversorDeExtratoService conversor;

  @Override
  public String banco() {
    return "Inter";
  }

  @Override
  public boolean reconhece(List<String> linhas) {
    return linhas.stream()
        .map(NormalizadorTexto::normalizarParaComparacao)
        .anyMatch(linha -> linha.startsWith("instituicao: banco inter"));
  }

  @Override
  public List<TransacaoLida> ler(List<String> linhas) {
    List<TransacaoLida> transacoes = new ArrayList<>();
    LocalDate dia = null;
    for (String linha : linhas) {
      Matcher cabecalhoDoDia = DIA.matcher(NormalizadorTexto.normalizarParaComparacao(linha));
      Matcher movimento = MOVIMENTO.matcher(linha);
      if (cabecalhoDoDia.matches()) {
        dia = dia(cabecalhoDoDia);
      } else if (dia != null && movimento.matches() && !conversor.ehSaldo(movimento.group(1))) {
        transacoes.add(
            conversor.transacao(dia, movimento.group(1), conversor.valor(movimento.group(2))));
      }
    }
    return transacoes;
  }

  private LocalDate dia(Matcher cabecalho) {
    int mes = MESES.indexOf(cabecalho.group(2)) + 1;
    if (mes == 0) {
      throw new BusinessException("O extrato contém uma data inválida.");
    }
    return conversor.data(
        "%02d/%02d/%s".formatted(Integer.parseInt(cabecalho.group(1)), mes, cabecalho.group(3)));
  }
}
