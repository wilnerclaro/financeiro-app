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
public class LeitorPdfSantanderService implements LeitorDeExtratoPdf {
  private static final Pattern MOVIMENTO =
      Pattern.compile(
          "^("
              + ConversorDeExtratoService.DATA
              + ")\\s*(.*?)\\s*("
              + ConversorDeExtratoService.VALOR
              + ")\\s+"
              + ConversorDeExtratoService.VALOR
              + "$");
  private static final Pattern DIA =
      Pattern.compile(
          "^(segunda|terca|quarta|quinta|sexta|sabado|domingo)(-feira)?, \\d{1,2} de \\p{L}+ de"
              + " \\d{4}$");
  private static final Pattern PAGINA = Pattern.compile("^\\d+/\\d+$");
  private static final String DETALHE = "detalhe dos movimentos";
  private static final String CABECALHO = "data descricao valor saldo";
  private final ConversorDeExtratoService conversor;

  @Override
  public String banco() {
    return "Santander";
  }

  @Override
  public boolean reconhece(List<String> linhas) {
    return conversor.contemLinha(linhas, "Extrato de conta")
        && conversor.contemLinha(linhas, DETALHE)
        && conversor.contemLinha(linhas, CABECALHO);
  }

  @Override
  public List<TransacaoLida> ler(List<String> linhas) {
    List<Movimento> movimentos = new ArrayList<>();
    List<String> pendentes = new ArrayList<>();
    boolean emMovimentos = false;
    for (String linha : linhas) {
      String normalizada = NormalizadorTexto.normalizarParaComparacao(linha);
      Matcher movimento = MOVIMENTO.matcher(linha);
      if (DETALHE.equals(normalizada)) {
        emMovimentos = true;
      } else if (!emMovimentos
          || CABECALHO.equals(normalizada)
          || DIA.matcher(normalizada).matches()
          || PAGINA.matcher(linha).matches()) {
        continue;
      } else if (movimento.matches()) {
        boolean semDescricao = movimento.group(2).isBlank();
        String tipo = semDescricao && !pendentes.isEmpty() ? pendentes.removeLast() : "";
        complementarUltimo(movimentos, pendentes);
        movimentos.add(
            new Movimento(
                movimento.group(1),
                semDescricao ? tipo : movimento.group(2),
                movimento.group(3),
                semDescricao));
      } else {
        pendentes.add(linha);
      }
    }
    complementarUltimo(movimentos, pendentes);
    return movimentos.stream()
        .filter(movimento -> !conversor.ehSaldo(movimento.descricao()))
        .map(
            movimento ->
                conversor.transacao(
                    conversor.data(movimento.data),
                    movimento.descricao(),
                    conversor.valor(movimento.valor)))
        .toList();
  }

  private void complementarUltimo(List<Movimento> movimentos, List<String> pendentes) {
    if (!movimentos.isEmpty() && !pendentes.isEmpty()) {
      movimentos.getLast().detalhes.addAll(pendentes);
    }
    pendentes.clear();
  }

  private static final class Movimento {
    private final String data;
    private final String tipo;
    private final String valor;
    private final boolean aceitaDetalhe;
    private final List<String> detalhes = new ArrayList<>();

    private Movimento(String data, String tipo, String valor, boolean aceitaDetalhe) {
      this.data = data;
      this.tipo = tipo;
      this.valor = valor;
      this.aceitaDetalhe = aceitaDetalhe;
    }

    private String descricao() {
      if (detalhes.isEmpty() || !aceitaDetalhe) {
        return tipo;
      }
      String detalhe = String.join(" ", detalhes.subList(0, Math.min(detalhes.size(), 2)));
      return tipo.isBlank() ? detalhe : tipo + " - " + detalhe;
    }
  }
}
