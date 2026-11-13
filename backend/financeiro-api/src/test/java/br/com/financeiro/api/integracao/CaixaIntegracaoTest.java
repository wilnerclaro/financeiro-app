package br.com.financeiro.api.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Caixa: cartão, transferência e atraso")
class CaixaIntegracaoTest extends IntegracaoTest {

  private Sessao usuario;
  private UUID mercado;

  @BeforeEach
  void prepararDados() throws Exception {
    usuario = criarUsuario();
    mercado = categoriaPadrao(usuario, "Supermercado");
  }

  @Test
  @DisplayName("cartão sem dia de fechamento e vencimento é recusado")
  void cartaoExigeDias() throws Exception {
    postar(
            usuario,
            "/api/contas",
            """
            {"nome":"Cartão","tipo":"CARTAO_CREDITO","saldoInicial":0}
            """)
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Cartão de crédito exige o dia de fechamento e o dia de vencimento da fatura."));
  }

  @Test
  @DisplayName("conta que não é cartão ignora os dias da fatura")
  void contaComumSemDias() throws Exception {
    postar(
            usuario,
            "/api/contas",
            """
            {"nome":"Banco","tipo":"CONTA_CORRENTE","saldoInicial":0,
             "diaFechamento":5,"diaVencimento":15}
            """)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.diaFechamento").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.diaVencimento").value(org.hamcrest.Matchers.nullValue()));
  }

  @Test
  @DisplayName("compra no cartão cai na fatura certa e vira competência do vencimento")
  void compraNoCartao() throws Exception {
    UUID cartao = criarCartao(usuario, "Cartão Azul", 10, 20);

    criarLancamentoEm(usuario, cartao, mercado, "DESPESA", "300.00", HOJE, null, null)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.dataCompra").value("2026-06-15"))
        .andExpect(jsonPath("$.dataCompetencia").value("2026-07-20"))
        .andExpect(jsonPath("$.dataVencimento").value("2026-07-20"))
        .andExpect(jsonPath("$.status").value("PENDENTE"));

    criarLancamentoEm(
            usuario, cartao, mercado, "DESPESA", "120.00", HOJE.withDayOfMonth(5), null, null)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.dataVencimento").value("2026-06-20"));

    consultar("/api/lancamentos-financeiros/faturas?contaId=" + cartao + "&ano=2026&mes=7", usuario)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dataFechamento").value("2026-07-10"))
        .andExpect(jsonPath("$.dataVencimento").value("2026-07-20"))
        .andExpect(jsonPath("$.fechada").value(false))
        .andExpect(jsonPath("$.total").value(300.00))
        .andExpect(jsonPath("$.lancamentos.length()").value(1));

    consultar("/api/lancamentos-financeiros/faturas?contaId=" + cartao + "&ano=2026&mes=6", usuario)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fechada").value(true))
        .andExpect(jsonPath("$.total").value(120.00));
  }

  @Test
  @DisplayName("fatura de conta que não é cartão é recusada")
  void faturaSoDeCartao() throws Exception {
    UUID banco = criarConta(usuario, "Banco", "0.00");
    consultar("/api/lancamentos-financeiros/faturas?contaId=" + banco + "&ano=2026&mes=6", usuario)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("A conta informada não é um cartão de crédito."));
  }

  @Test
  @DisplayName("transferência move o saldo das duas contas e o cancelamento devolve")
  void transferencia() throws Exception {
    UUID origem = criarConta(usuario, "Banco", "1000.00");
    UUID destino = criarConta(usuario, "Poupança", "0.00");

    UUID id =
        idDe(
            postar(usuario, "/api/transferencias", corpoTransferencia(origem, destino, "300.00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valor").value(300.00)));

    assertThat(saldoAtual(origem, usuario)).isEqualByComparingTo("700.00");
    assertThat(saldoAtual(destino, usuario)).isEqualByComparingTo("300.00");

    consultar("/api/transferencias?contaId=" + destino, usuario)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements").value(1));

    remover("/api/transferencias/" + id, usuario).andExpect(status().isNoContent());
    assertThat(saldoAtual(origem, usuario)).isEqualByComparingTo("1000.00");
    assertThat(saldoAtual(destino, usuario)).isEqualByComparingTo("0.00");

    remover("/api/transferencias/" + id, usuario).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("transferência recusa contas iguais, data futura e conta de outro usuário")
  void transferenciaInvalida() throws Exception {
    UUID conta = criarConta(usuario, "Banco", "1000.00");
    UUID outra = criarConta(usuario, "Poupança", "0.00");

    postar(usuario, "/api/transferencias", corpoTransferencia(conta, conta, "10.00"))
        .andExpect(status().isBadRequest());

    postar(
            usuario,
            "/api/transferencias",
            """
            {"contaOrigemId":"%s","contaDestinoId":"%s","valor":10.00,
             "dataTransferencia":"%s"}
            """
                .formatted(conta, outra, HOJE.plusDays(1)))
        .andExpect(status().isBadRequest());

    Sessao intruso = criarUsuario();
    UUID contaDoIntruso = criarConta(intruso, "Banco", "0.00");
    postar(usuario, "/api/transferencias", corpoTransferencia(conta, contaDoIntruso, "10.00"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("pendente vencido aparece como ATRASADO e o filtro separa atrasado de pendente")
  void atrasadoCalculadoNaLeitura() throws Exception {
    UUID banco = criarConta(usuario, "Banco", "0.00");
    UUID vencido =
        idDe(
            criarLancamento(usuario, banco, mercado, "DESPESA", "50.00", HOJE.minusDays(1), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ATRASADO")));
    criarLancamento(usuario, banco, mercado, "DESPESA", "70.00", HOJE.plusDays(3), null)
        .andExpect(jsonPath("$.status").value("PENDENTE"));

    consultar("/api/lancamentos-financeiros/" + vencido, usuario)
        .andExpect(jsonPath("$.status").value("ATRASADO"));

    mockMvc
        .perform(
            autenticado(get("/api/lancamentos-financeiros"), usuario).param("status", "ATRASADO"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].valor").value(50.00));

    mockMvc
        .perform(
            autenticado(get("/api/lancamentos-financeiros"), usuario).param("status", "PENDENTE"))
        .andExpect(jsonPath("$.page.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].valor").value(70.00));
  }

  private String corpoTransferencia(UUID origem, UUID destino, String valor) {
    return """
        {"contaOrigemId":"%s","contaDestinoId":"%s","valor":%s,"dataTransferencia":"%s",
         "descricao":"Reserva"}
        """
        .formatted(origem, destino, valor, HOJE);
  }
}
