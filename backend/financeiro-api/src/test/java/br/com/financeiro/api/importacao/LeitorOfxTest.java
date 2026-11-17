package br.com.financeiro.api.importacao;

import static br.com.financeiro.api.importacao.OfxDeTeste.documento;
import static br.com.financeiro.api.importacao.OfxDeTeste.transacao;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.importacao.service.LeitorOfxService;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LeitorOfxTest {
  private final LeitorOfxService leitor =
      new LeitorOfxService(
          Clock.fixed(Instant.parse("2026-06-15T15:00:00Z"), ZoneId.of("America/Sao_Paulo")));

  @Test
  void leSgmlComPrecisaoExataETextoAcentuado() {
    var itens =
        leitor.ler(
            documento(transacao("a", "-15.0000") + transacao("b", "25.50"))
                .getBytes(StandardCharsets.UTF_8));
    assertThat(itens).hasSize(2);
    assertThat(itens.getFirst().valor()).isEqualByComparingTo("15.00");
    assertThat(itens.getFirst().tipo().name()).isEqualTo("DESPESA");
    assertThat(itens.getFirst().descricao()).isEqualTo("Compra de café");
    assertThat(itens.getFirst().data()).hasToString("2026-06-10");
    assertThat(itens.get(1).tipo().name()).isEqualTo("RECEITA");
  }

  @Test
  void leXmlEWindows1252() {
    String xml =
        documento(transacao("a", "-15"))
            .replace("BRL\n", "BRL</CURDEF>")
            .replace("001\n", "001</BANKID>")
            .replace("12345\n", "12345</ACCTID>")
            .replace("CHECKING\n", "CHECKING</ACCTTYPE>")
            .replace("DEBIT\n", "DEBIT</TRNTYPE>")
            .replace("[-3:BRT]\n", "[-3:BRT]</DTPOSTED>")
            .replace("-15\n", "-15</TRNAMT>")
            .replace("a\n", "a</FITID>")
            .replace("Mercado\n", "Mercado</NAME>")
            .replace("café\n", "café</MEMO>");
    assertThat(leitor.ler(xml.getBytes(StandardCharsets.UTF_8))).hasSize(1);
    String legado =
        documento(transacao("a", "-15"))
            .replace("ENCODING:UTF-8", "ENCODING:USASCII\nCHARSET:1252");
    assertThat(leitor.ler(legado.getBytes(Charset.forName("windows-1252"))).getFirst().descricao())
        .contains("café");
  }

  @ParameterizedTest
  @ValueSource(strings = {"-1.001", "100000000000000000.00", "texto", "NaN"})
  void recusaValorInvalido(String valor) {
    assertThatThrownBy(
            () -> leitor.ler(documento(transacao("a", valor)).getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void recusaArquivoVazioGrandeCorrecaoEIdentificadorRepetido() {
    for (String texto :
        new String[] {
          "",
          documento(transacao("a", "-1") + transacao("a", "-1")),
          documento(transacao("a", "-1").replace("<NAME>", "<CORRECTFITID>anterior\n<NAME>")),
          documento(transacao("a", "-1")).replace("<OFX>", "<!DOCTYPE x><OFX>"),
          documento(transacao("a", "-1")).replace("BRL", "USD"),
          documento(transacao("a", "-1")).replace("20260610", "20260231"),
          documento(transacao("a", "-1")).replace("<FITID>a\n", ""),
          documento(transacao("a", "-1")).replace("<STMTRS>", "<CCSTMTRS>"),
          documento(transacao("a", "-1")).replace("</STMTTRN>", "")
        }) {
      assertThatThrownBy(() -> leitor.ler(texto.getBytes(StandardCharsets.UTF_8)))
          .isInstanceOf(BusinessException.class);
    }
    assertThatThrownBy(() -> leitor.ler(new byte[LeitorOfxService.LIMITE_BYTES + 1]))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void recusaEstruturaTruncadaTransacaoForaDoExtratoEMoedaConvertida() {
    String valido = documento(transacao("a", "-1"));
    for (String invalido :
        new String[] {
          valido.replace("</STMTRS>", ""),
          valido.replace("</BANKTRANLIST>", ""),
          valido.replace("</OFX>", transacao("b", "-2") + "</OFX>"),
          valido.replace("<NAME>", "<CURRENCY><CURSYM>USD</CURSYM></CURRENCY><NAME>"),
          valido.replace("<NAME>", "<ORIGCURRENCY><CURSYM>USD</CURSYM></ORIGCURRENCY><NAME>")
        }) {
      assertThatThrownBy(() -> leitor.ler(invalido.getBytes(StandardCharsets.UTF_8)))
          .isInstanceOf(BusinessException.class);
    }
  }

  @Test
  void transferenciaFuturoEZeroExigemRevisaoManual() {
    for (String transacao :
        new String[] {
          transacao("a", "-1").replace("DEBIT", "XFER"),
          transacao("a", "-1").replace("20260610", "20260620"),
          transacao("a", "0"),
          transacao("a", "-1")
              .replace("<NAME>", "<BANKACCTTO><ACCTID>678</ACCTID></BANKACCTTO><NAME>")
        }) {
      var item = leitor.ler(documento(transacao).getBytes(StandardCharsets.UTF_8)).getFirst();
      assertThat(item.importavel()).isFalse();
      assertThat(item.motivo()).isNotBlank();
    }
  }
}
