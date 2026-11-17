package br.com.financeiro.api.importacao.service;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.importacao.dto.ItemOfxResponse;
import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LeitorOfxService {
  public static final int LIMITE_BYTES = 2 * 1024 * 1024;
  private static final Pattern TRANSACAO =
      Pattern.compile("<STMTTRN>\\s*([\\s\\S]*?)</STMTTRN>", Pattern.CASE_INSENSITIVE);
  private static final DateTimeFormatter DATA = DateTimeFormatter.BASIC_ISO_DATE;
  private final Clock clock;

  public List<ItemOfxResponse> ler(byte[] arquivo) {
    if (arquivo.length == 0 || arquivo.length > LIMITE_BYTES) {
      throw new BusinessException("Envie um arquivo OFX de até 2 MB.");
    }
    String texto = decodificar(arquivo);
    String maiusculo = texto.toUpperCase(Locale.ROOT);
    validarDocumento(maiusculo);
    String extrato = agregado(texto, "STMTRS");
    if (!"BRL".equals(campo(extrato, "CURDEF", true))) {
      throw new BusinessException("A importação aceita somente extratos em reais (BRL).");
    }
    String conta = agregado(extrato, "BANKACCTFROM");
    String origem = campo(conta, "BANKID", true) + "\u0000" + campo(conta, "ACCTID", true);
    String transacoes = agregado(extrato, "BANKTRANLIST");
    List<ItemOfxResponse> itens = lerTransacoes(transacoes, origem);
    if (itens.size() != ocorrencias(maiusculo, "<STMTTRN>")) {
      throw new BusinessException("O OFX contém transações fora do extrato ou incompletas.");
    }
    return itens;
  }

  private void validarDocumento(String maiusculo) {
    if (!maiusculo.contains("<OFX>")
        || !maiusculo.contains("</OFX>")
        || ocorrencias(maiusculo, "<STMTRS>") != 1
        || ocorrencias(maiusculo, "<BANKACCTFROM>") != 1
        || maiusculo.contains("<CCSTMTRS>")
        || maiusculo.contains("<INVSTMTRS>")) {
      throw new BusinessException(
          "Envie um OFX com um único extrato bancário. Cartões e investimentos ainda não são aceitos.");
    }
    if (maiusculo.contains("<!DOCTYPE")
        || maiusculo.contains("<!ENTITY")
        || maiusculo.contains("<STMTTRNP>")
        || maiusculo.contains("<BANKTRANLISTP>")) {
      throw new BusinessException("O arquivo contém estruturas OFX não suportadas.");
    }
  }

  private String agregado(String texto, String tag) {
    String maiusculo = texto.toUpperCase(Locale.ROOT);
    String abertura = "<" + tag + ">";
    String fechamento = "</" + tag + ">";
    int inicio = maiusculo.indexOf(abertura);
    int fim = maiusculo.indexOf(fechamento);
    if (inicio < 0
        || fim <= inicio
        || ocorrencias(maiusculo, abertura) != 1
        || ocorrencias(maiusculo, fechamento) != 1) {
      throw new BusinessException(
          "O OFX contém estrutura ausente, repetida ou incompleta: " + tag + ".");
    }
    return texto.substring(inicio + abertura.length(), fim);
  }

  private List<ItemOfxResponse> lerTransacoes(String texto, String origem) {
    Matcher matcher = TRANSACAO.matcher(texto);
    List<ItemOfxResponse> itens = new ArrayList<>();
    Set<String> identificadores = new HashSet<>();
    while (matcher.find()) {
      if (itens.size() >= 1000) {
        throw new BusinessException("O arquivo deve ter no máximo 1.000 transações.");
      }
      String bloco = matcher.group(1);
      String identificador = hash(origem + "\u0000" + campo(bloco, "FITID", true));
      if (!identificadores.add(identificador)) {
        throw new BusinessException("O arquivo contém identificadores de transação repetidos.");
      }
      itens.add(item(bloco, identificador));
    }
    if (itens.isEmpty()
        || itens.size() != ocorrencias(texto.toUpperCase(Locale.ROOT), "<STMTTRN>")) {
      throw new BusinessException("O OFX não contém transações válidas ou está incompleto.");
    }
    return List.copyOf(itens);
  }

  private ItemOfxResponse item(String bloco, String identificador) {
    String maiusculo = bloco.toUpperCase(Locale.ROOT);
    if (maiusculo.contains("<CORRECTFITID>") || maiusculo.contains("<CORRECTACTION>")) {
      throw new BusinessException("Correções e exclusões OFX precisam ser revisadas manualmente.");
    }
    if (maiusculo.contains("<CURRENCY>") || maiusculo.contains("<ORIGCURRENCY>")) {
      throw new BusinessException(
          "Transações com conversão de moeda precisam ser revisadas manualmente.");
    }
    LocalDate data = data(campo(bloco, "DTPOSTED", true));
    BigDecimal valor = valor(campo(bloco, "TRNAMT", true));
    String motivo = motivo(bloco, data, valor);
    return new ItemOfxResponse(
        identificador,
        data,
        descricao(bloco),
        valor.abs(),
        valor.signum() < 0 ? TipoLancamentoFinanceiro.DESPESA : TipoLancamentoFinanceiro.RECEITA,
        false,
        motivo == null,
        motivo);
  }

  private LocalDate data(String dataTexto) {
    try {
      if (!dataTexto.matches("[0-9]{8}([0-9]{6}(\\.[0-9]+)?(\\[[^\\]]+\\])?)?")) {
        throw new IllegalArgumentException();
      }
      LocalDate data = LocalDate.parse(dataTexto.substring(0, 8), DATA);
      if (data.getYear() < 2000 || data.getYear() > 2100) {
        throw new IllegalArgumentException();
      }
      return data;
    } catch (IllegalArgumentException | DateTimeParseException exception) {
      throw new BusinessException("O OFX contém data inválida.");
    }
  }

  private BigDecimal valor(String numero) {
    try {
      if (!numero.matches("[+-]?[0-9]+(\\.[0-9]+)?")) {
        throw new IllegalArgumentException();
      }
      BigDecimal valor = new BigDecimal(numero).setScale(2, RoundingMode.UNNECESSARY);
      if (valor.precision() > 19) {
        throw new IllegalArgumentException();
      }
      return valor;
    } catch (IllegalArgumentException | ArithmeticException exception) {
      throw new BusinessException("O OFX contém data ou valor monetário inválido.");
    }
  }

  private String descricao(String bloco) {
    String nome = campo(bloco, "NAME", false);
    String memo = campo(bloco, "MEMO", false);
    String descricao = !memo.isBlank() ? memo : !nome.isBlank() ? nome : "Movimentação bancária";
    descricao =
        descricao
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'");
    return descricao.substring(0, Math.min(descricao.length(), 180));
  }

  private String motivo(String bloco, LocalDate data, BigDecimal valor) {
    String tipoBanco = campo(bloco, "TRNTYPE", true).toUpperCase(Locale.ROOT);
    String maiusculo = bloco.toUpperCase(Locale.ROOT);
    if ("XFER".equals(tipoBanco)
        || maiusculo.contains("<BANKACCTTO>")
        || maiusculo.contains("<CCACCTTO>")) {
      return "Registre esta transferência na tela de lançamentos.";
    } else if (data.isAfter(LocalDate.now(clock))) {
      return "Data futura: revise e registre manualmente.";
    } else if (valor.signum() == 0) {
      return "Transações com valor zero não são importadas.";
    }
    return null;
  }

  private String campo(String texto, String tag, boolean obrigatorio) {
    Matcher matcher =
        Pattern.compile("<" + tag + ">([^<]*)", Pattern.CASE_INSENSITIVE).matcher(texto);
    if (!matcher.find()) {
      if (obrigatorio) {
        throw new BusinessException("O OFX não contém o campo obrigatório " + tag + ".");
      }
      return "";
    }
    String valor = matcher.group(1).trim();
    if (valor.isBlank() && obrigatorio) {
      throw new BusinessException("O OFX contém um campo obrigatório vazio: " + tag + ".");
    }
    if (matcher.find()) {
      throw new BusinessException("O OFX contém campos duplicados: " + tag + ".");
    }
    return valor;
  }

  private String decodificar(byte[] arquivo) {
    String cabecalho =
        new String(arquivo, 0, Math.min(arquivo.length, 512), StandardCharsets.ISO_8859_1)
            .toUpperCase(Locale.ROOT);
    Charset charset =
        cabecalho.contains("CHARSET:1252")
                || cabecalho.contains("WINDOWS-1252")
                || cabecalho.contains("ISO-8859-1")
            ? Charset.forName("windows-1252")
            : StandardCharsets.UTF_8;
    try {
      return charset
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(arquivo))
          .toString();
    } catch (CharacterCodingException exception) {
      throw new BusinessException(
          "Codificação do OFX não reconhecida. Exporte em UTF-8 ou Windows-1252.");
    }
  }

  private int ocorrencias(String texto, String termo) {
    return (texto.length() - texto.replace(termo, "").length()) / termo.length();
  }

  private String hash(String texto) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
