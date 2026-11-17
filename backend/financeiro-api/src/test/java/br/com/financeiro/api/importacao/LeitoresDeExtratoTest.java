package br.com.financeiro.api.importacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.importacao.dto.TransacaoLida;
import br.com.financeiro.api.importacao.service.ConversorDeExtratoService;
import br.com.financeiro.api.importacao.service.LeitorCsvInterService;
import br.com.financeiro.api.importacao.service.LeitorCsvSantanderService;
import br.com.financeiro.api.importacao.service.LeitorPdfInterService;
import br.com.financeiro.api.importacao.service.LeitorPdfItauService;
import br.com.financeiro.api.importacao.service.LeitorPdfSantanderService;
import br.com.financeiro.api.importacao.service.TabelaDeExtratoService;
import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class LeitoresDeExtratoTest {
  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
  private static final TipoLancamentoFinanceiro RECEITA = TipoLancamentoFinanceiro.RECEITA;
  private static final TipoLancamentoFinanceiro DESPESA = TipoLancamentoFinanceiro.DESPESA;

  private final ConversorDeExtratoService conversor =
      new ConversorDeExtratoService(
          Clock.fixed(LocalDate.of(2026, 6, 15).atStartOfDay(FUSO).toInstant(), FUSO));
  private final TabelaDeExtratoService tabela = new TabelaDeExtratoService(conversor);
  private final LeitorPdfItauService itau = new LeitorPdfItauService(conversor);
  private final LeitorPdfSantanderService santanderPdf = new LeitorPdfSantanderService(conversor);
  private final LeitorPdfInterService interPdf = new LeitorPdfInterService(conversor);
  private final LeitorCsvSantanderService santanderCsv = new LeitorCsvSantanderService(tabela);
  private final LeitorCsvInterService interCsv = new LeitorCsvInterService(tabela);

  @Test
  void converteValoresEDatasNosFormatosDosBancos() {
    assertThat(conversor.valor("-R$ 5.069,68")).isEqualByComparingTo("-5069.68");
    assertThat(conversor.valor("R$ 800,00")).isEqualByComparingTo("800");
    assertThat(conversor.valor("R$ -12,30")).isEqualByComparingTo("-12.30");
    assertThat(conversor.valor("1.821,47")).isEqualByComparingTo("1821.47");
    assertThat(conversor.valor("-0,01")).isEqualByComparingTo("-0.01");
    assertThatThrownBy(() -> conversor.valor("12.34")).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> conversor.valor("--R$ 1,00")).isInstanceOf(BusinessException.class);
    assertThat(conversor.data("29/02/2028")).isEqualTo(LocalDate.of(2028, 2, 29));
    assertThatThrownBy(() -> conversor.data("30/02/2026"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("data inválida");
    assertThatThrownBy(() -> conversor.data("10/06/1999"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("período");
    assertThat(conversor.ehSaldo("SALDO ANTERIOR")).isTrue();
    assertThat(conversor.ehSaldo("Saldo")).isTrue();
    assertThat(conversor.ehSaldo("Saldão da loja")).isFalse();
  }

  @Test
  void marcaDataFuturaEValorZeroComoNaoImportaveis() {
    TransacaoLida futura =
        conversor.transacao(LocalDate.of(2026, 6, 16), "  Boleto   futuro ", new BigDecimal("-1"));
    assertThat(futura.descricao()).isEqualTo("Boleto futuro");
    assertThat(futura.importavel()).isFalse();
    assertThat(futura.motivo()).contains("Data futura");
    TransacaoLida zero = conversor.transacao(LocalDate.of(2026, 6, 15), " ", BigDecimal.ZERO);
    assertThat(zero.descricao()).isEqualTo("Movimentação bancária");
    assertThat(zero.motivo()).contains("valor zero");
    assertThat(conversor.transacao(LocalDate.of(2026, 6, 1), "x".repeat(300), BigDecimal.ONE))
        .extracting(transacao -> transacao.descricao().length(), TransacaoLida::tipo)
        .containsExactly(180, RECEITA);
  }

  @Test
  void leExtratoDoItauIgnorandoFuturosSaldosDoDiaECabecalhosDePagina() {
    List<String> linhas =
        List.of(
            "Cliente Exemplo",
            "Extrato conta corrente",
            "Período de visualização: 01/06/2026 até 15/06/2026",
            "Lançamentos futuros",
            "Saídas futuras",
            "Data Lançamentos Descrição Valor (R$) Saldo (R$)",
            "30/06/2026 SAÍDA FINANCIAMENTO EXEMPLO -900,00",
            "Lançamentos",
            "Data Lançamentos Descrição Valor (R$) Saldo (R$)",
            "12/06/2026 1.250,10",
            "12/06/2026 PAGAMENTO DE BOLETO CONDOMINIO EXEMPLO -831,64",
            "ADMINISTRADORA",
            "12/06/2026 PAGAMENTO DE PIX QR PADARIA EXEMPLO -37,45",
            "CODE",
            "Data Lançamentos Descrição Valor (R$) Saldo (R$)",
            "11/06/2026 PAGAMENTO DE PIX QR FULANA DE -30,00",
            "CODE TAL SOUZA",
            "11/06/2026 RENDIMENTOS REND PAGO APLIC AUT 0,06",
            "MAIS",
            "10/06/2026 PIX RECEBIDO EMPRESA EXEMPLO 1.821,47",
            "01/06/2026 SALDO ANTERIOR 900,00",
            "Posição consolidada",
            "saldo disponível R$ 1,00",
            "Consulte itau.com.br");
    assertThat(itau.reconhece(linhas)).isTrue();
    assertThat(itau.banco()).isEqualTo("Itaú");
    assertThat(itau.ler(linhas))
        .extracting(
            TransacaoLida::data,
            TransacaoLida::descricao,
            TransacaoLida::valor,
            TransacaoLida::tipo)
        .containsExactly(
            tuple(
                LocalDate.of(2026, 6, 12),
                "PAGAMENTO DE BOLETO CONDOMINIO EXEMPLO ADMINISTRADORA",
                new BigDecimal("831.64"),
                DESPESA),
            tuple(
                LocalDate.of(2026, 6, 12),
                "PAGAMENTO DE PIX QR CODE PADARIA EXEMPLO",
                new BigDecimal("37.45"),
                DESPESA),
            tuple(
                LocalDate.of(2026, 6, 11),
                "PAGAMENTO DE PIX QR CODE FULANA DE TAL SOUZA",
                new BigDecimal("30.00"),
                DESPESA),
            tuple(
                LocalDate.of(2026, 6, 11),
                "RENDIMENTOS REND PAGO APLIC AUT MAIS",
                new BigDecimal("0.06"),
                RECEITA),
            tuple(
                LocalDate.of(2026, 6, 10),
                "PIX RECEBIDO EMPRESA EXEMPLO",
                new BigDecimal("1821.47"),
                RECEITA));
  }

  @Test
  void naoConfundeOutrosPdfsComOExtratoDoItau() {
    assertThat(itau.reconhece(List.of("Extrato conta corrente", "Banco Exemplo"))).isFalse();
    assertThat(itau.reconhece(List.of("Extrato conta corrente", "itau.com.br"))).isFalse();
    assertThat(itau.ler(List.of("10/06/2026 PIX ENVIADO SEM SECAO -1,00"))).isEmpty();
  }

  @Test
  void leExtratoDoSantanderComTipoAcimaEDetalheAbaixoDaData() {
    List<String> linhas =
        List.of(
            "EXTRATO DE CONTA",
            "Agência: 0001 | Conta: 01.000000.0",
            "Saldo inicial Entradas Saídas Saldo final",
            "R$ 100,00 R$ 900,00 R$ 150,00 R$ 850,00",
            "DETALHE DOS MOVIMENTOS",
            "Data Descrição Valor Saldo",
            "Segunda, 15 de junho de 2026",
            "Pix recebido",
            "15/06/2026 R$ 800,00 R$ 850,00",
            "Fulano De Tal",
            "Sexta, 12 de junho de 2026",
            "12/06/2026 Remuneracao aplicacao automatica R$ 0,03 R$ 50,00",
            "Compra cartao deb mc",
            "12/06/2026 -R$ 100,00 R$ 49,97",
            "1/2",
            "DETALHE DOS MOVIMENTOS",
            "Data Descrição Valor Saldo",
            "Mercado Exemplo",
            "Filial Centro",
            "Pix enviado",
            "11/06/2026 -R$ 50,03 R$ 149,97",
            "Loja Exemplo Ltda",
            "11/06/2026 Saldo anterior R$ 200,00 R$ 200,00",
            "2/2");
    assertThat(santanderPdf.reconhece(linhas)).isTrue();
    assertThat(santanderPdf.banco()).isEqualTo("Santander");
    assertThat(santanderPdf.ler(linhas))
        .extracting(TransacaoLida::data, TransacaoLida::descricao, TransacaoLida::valor)
        .containsExactly(
            tuple(
                LocalDate.of(2026, 6, 15),
                "Pix recebido - Fulano De Tal",
                new BigDecimal("800.00")),
            tuple(
                LocalDate.of(2026, 6, 12),
                "Remuneracao aplicacao automatica",
                new BigDecimal("0.03")),
            tuple(
                LocalDate.of(2026, 6, 12),
                "Compra cartao deb mc - Mercado Exemplo Filial Centro",
                new BigDecimal("100.00")),
            tuple(
                LocalDate.of(2026, 6, 11),
                "Pix enviado - Loja Exemplo Ltda",
                new BigDecimal("50.03")));
    assertThat(santanderPdf.reconhece(List.of("EXTRATO DE CONTA", "Data Descrição Valor Saldo")))
        .isFalse();
  }

  @Test
  void leCsvDoSantanderAPartirDoCabecalho() {
    List<List<String>> linhas =
        List.of(
            List.of("Extrato de conta corrente"),
            List.of("Cliente", "Cliente Exemplo"),
            List.of("Lançamentos:"),
            List.of("Data", "Descrição", "Detalhe", "Valor", "Saldo"),
            List.of("15/06/2026", "Pix recebido", "Fulano De Tal", "R$ 800,00", "R$ 850,00"),
            List.of("12/06/2026", "Remuneracao aplicacao automatica", "", "R$ 0,03", "R$ 50,00"),
            List.of(""),
            List.of("12/06/2026", "Compra cartao deb mc", "11/06 Mercado", "-R$ 1.100,00", "R$ 0"),
            List.of("Total", "", "", "R$ 0,00", ""));
    assertThat(santanderCsv.reconhece(linhas)).isTrue();
    assertThat(santanderCsv.banco()).isEqualTo("Santander");
    assertThat(santanderCsv.ler(linhas))
        .extracting(TransacaoLida::descricao, TransacaoLida::valor, TransacaoLida::tipo)
        .containsExactly(
            tuple("Pix recebido - Fulano De Tal", new BigDecimal("800.00"), RECEITA),
            tuple("Remuneracao aplicacao automatica", new BigDecimal("0.03"), RECEITA),
            tuple("Compra cartao deb mc - 11/06 Mercado", new BigDecimal("1100.00"), DESPESA));
    assertThat(santanderCsv.reconhece(List.of(List.of("Data", "Descrição", "Valor")))).isFalse();
    assertThatThrownBy(
            () ->
                santanderCsv.ler(
                    List.of(
                        List.of("Data", "Descrição", "Detalhe", "Valor", "Saldo"),
                        List.of("15/06/2026", "Pix", "Fulano"))))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("incompleta");
  }

  @Test
  void leCsvDoInter() {
    List<List<String>> linhas =
        List.of(
            List.of(" Extrato Conta Corrente "),
            List.of("Conta ", "00000000"),
            List.of("Data Lançamento", "Histórico", "Descrição", "Valor", "Saldo"),
            List.of("10/06/2026", "Pix enviado ", "Fulano De Tal", "-12,00", "21,00"),
            List.of("09/06/2026", "Compra no débito", "Padaria    Exemplo   Bra", "-4,50", "33,00"),
            List.of("08/06/2026", "Estorno", "", "1.000,00", "37,50"));
    assertThat(interCsv.reconhece(linhas)).isTrue();
    assertThat(interCsv.banco()).isEqualTo("Inter");
    assertThat(interCsv.ler(linhas))
        .extracting(TransacaoLida::descricao, TransacaoLida::valor, TransacaoLida::tipo)
        .containsExactly(
            tuple("Pix enviado - Fulano De Tal", new BigDecimal("12.00"), DESPESA),
            tuple("Compra no débito - Padaria Exemplo Bra", new BigDecimal("4.50"), DESPESA),
            tuple("Estorno", new BigDecimal("1000.00"), RECEITA));
    assertThat(santanderCsv.reconhece(linhas)).isFalse();
  }

  @Test
  void leExtratoDoInterAgrupadoPorDia() {
    List<String> linhas =
        Arrays.asList(
            "Instituição: Banco Inter,",
            "Saldo total Saldo disponível: Saldo bloqueado:",
            "R$ 21,00 R$ 21,00 R$ 0,00",
            "9 de Junho de 2026 Saldo do dia: R$ 95,00 Valor Saldo por transação",
            "Pix enviado: \"Fulano De Tal\" -R$ 100,00 R$ 100,00",
            "Compra no debito: \"No estabelecimento PADARIA\" -R$ 5,00 R$ 95,00",
            "10 de Março de 2026 Saldo do dia: R$ 1.095,00",
            "Fale com a gente",
            "Pix recebido: \"Empresa Exemplo\" R$ 1.000,00 R$ 1.095,00");
    assertThat(interPdf.reconhece(linhas)).isTrue();
    assertThat(interPdf.banco()).isEqualTo("Inter");
    assertThat(interPdf.ler(linhas))
        .extracting(TransacaoLida::data, TransacaoLida::descricao, TransacaoLida::tipo)
        .containsExactly(
            tuple(LocalDate.of(2026, 6, 9), "Pix enviado: \"Fulano De Tal\"", DESPESA),
            tuple(
                LocalDate.of(2026, 6, 9),
                "Compra no debito: \"No estabelecimento PADARIA\"",
                DESPESA),
            tuple(LocalDate.of(2026, 3, 10), "Pix recebido: \"Empresa Exemplo\"", RECEITA));
    assertThat(interPdf.reconhece(List.of("Banco Exemplo"))).isFalse();
    assertThatThrownBy(() -> interPdf.ler(List.of("9 de Brumário de 2026 Saldo do dia: R$ 1,00")))
        .isInstanceOf(BusinessException.class);
  }
}
