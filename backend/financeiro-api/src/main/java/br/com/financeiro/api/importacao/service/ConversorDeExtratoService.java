package br.com.financeiro.api.importacao.service;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.util.NormalizadorTexto;
import br.com.financeiro.api.importacao.dto.TransacaoLida;
import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConversorDeExtratoService {
  public static final String VALOR = "-?\\s?(?:R\\$\\s?)?-?\\d{1,3}(?:\\.\\d{3})*,\\d{2}";
  public static final String DATA = "\\d{2}/\\d{2}/\\d{4}";
  private static final Pattern PARTES_DO_VALOR =
      Pattern.compile("(-)?\\s?(?:R\\$\\s?)?(-)?(\\d{1,3}(?:\\.\\d{3})*),(\\d{2})");
  private static final DateTimeFormatter FORMATO_DATA =
      DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
  private static final int TAMANHO_DESCRICAO = 180;
  private final Clock clock;

  public BigDecimal valor(String texto) {
    Matcher partes = PARTES_DO_VALOR.matcher(texto.strip());
    if (!partes.matches() || partes.group(1) != null && partes.group(2) != null) {
      throw new BusinessException("O extrato contém um valor em formato inesperado.");
    }
    BigDecimal valor = new BigDecimal(partes.group(3).replace(".", "") + "." + partes.group(4));
    return partes.group(1) != null || partes.group(2) != null ? valor.negate() : valor;
  }

  public LocalDate data(String texto) {
    try {
      LocalDate data = LocalDate.parse(texto.strip(), FORMATO_DATA);
      if (data.getYear() < 2000 || data.getYear() > 2100) {
        throw new BusinessException("O extrato contém data fora do período aceito.");
      }
      return data;
    } catch (DateTimeParseException exception) {
      throw new BusinessException("O extrato contém uma data inválida.");
    }
  }

  public boolean ehData(String texto) {
    return texto != null && texto.strip().matches(DATA);
  }

  public boolean ehSaldo(String descricao) {
    String normalizada = NormalizadorTexto.normalizarParaComparacao(descricao);
    return normalizada.equals("saldo") || normalizada.startsWith("saldo ");
  }

  public boolean contemLinha(List<String> linhas, String procurada) {
    String normalizada = NormalizadorTexto.normalizarParaComparacao(procurada);
    return linhas.stream()
        .anyMatch(linha -> NormalizadorTexto.normalizarParaComparacao(linha).equals(normalizada));
  }

  public TransacaoLida transacao(LocalDate data, String descricao, BigDecimal valorComSinal) {
    String texto = NormalizadorTexto.normalizarNome(descricao);
    if (texto.isEmpty()) {
      texto = "Movimentação bancária";
    }
    return new TransacaoLida(
        null,
        data,
        texto.substring(0, Math.min(texto.length(), TAMANHO_DESCRICAO)),
        valorComSinal.abs(),
        valorComSinal.signum() < 0
            ? TipoLancamentoFinanceiro.DESPESA
            : TipoLancamentoFinanceiro.RECEITA,
        motivo(data, valorComSinal));
  }

  private String motivo(LocalDate data, BigDecimal valor) {
    if (data.isAfter(LocalDate.now(clock))) {
      return "Data futura: revise e registre manualmente.";
    }
    if (valor.signum() == 0) {
      return "Transações com valor zero não são importadas.";
    }
    return null;
  }
}
