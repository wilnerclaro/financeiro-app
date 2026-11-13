package br.com.financeiro.api.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Lançamentos financeiros e saldo da conta")
class LancamentoFinanceiroIntegracaoTest extends IntegracaoTest {

  private static final String URL = "/api/lancamentos-financeiros";

  private UUID usuarioId;
  private UUID contaId;
  private UUID despesaId;
  private UUID receitaId;

  @BeforeEach
  void prepararDados() throws Exception {
    usuarioId = criarUsuario();
    contaId = criarConta(usuarioId, "Banco Principal", "1000.00");
    despesaId = criarCategoria(usuarioId, "Mercado", "DESPESA", null);
    receitaId = criarCategoria(usuarioId, "Salário", "RECEITA", null);
  }

  @Test
  @DisplayName("despesa paga na criação debita o saldo e devolve as datas de auditoria")
  void criarDespesaPaga() throws Exception {
    criarLancamento(usuarioId, contaId, despesaId, "DESPESA", "150.00", null, HOJE)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PAGO"))
        .andExpect(jsonPath("$.criadoEm").isNotEmpty())
        .andExpect(jsonPath("$.atualizadoEm").isNotEmpty());

    assertThat(saldoAtual(contaId, usuarioId)).isEqualByComparingTo("850.00");
  }

  @Test
  @DisplayName("receita pendente não mexe no saldo até ser paga")
  void pagarReceitaPendente() throws Exception {
    UUID id =
        idDe(
            criarLancamento(
                    usuarioId, contaId, receitaId, "RECEITA", "500.00", HOJE.plusDays(5), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE")));

    assertThat(saldoAtual(contaId, usuarioId)).isEqualByComparingTo("1000.00");

    alterar(URL + "/" + id + "/pagar", corpoPagamento())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PAGO"));

    assertThat(saldoAtual(contaId, usuarioId)).isEqualByComparingTo("1500.00");

    alterar(URL + "/" + id + "/pagar", corpoPagamento())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("O lançamento informado já está pago."));
  }

  @Test
  @DisplayName("vencimento no passado sem pagamento nasce como ATRASADO")
  void lancamentoAtrasado() throws Exception {
    criarLancamento(usuarioId, contaId, despesaId, "DESPESA", "80.00", HOJE.minusDays(1), null)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("ATRASADO"));
  }

  @Test
  @DisplayName("data de pagamento no futuro é rejeitada")
  void pagamentoFuturo() throws Exception {
    criarLancamento(usuarioId, contaId, despesaId, "DESPESA", "80.00", null, HOJE.plusDays(1))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("A data de pagamento não pode ser futura."));
  }

  @Test
  @DisplayName("categoria de receita não pode ser usada em despesa")
  void categoriaIncompativel() throws Exception {
    criarLancamento(usuarioId, contaId, receitaId, "DESPESA", "80.00", null, null)
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("A categoria informada não é compatível com o tipo do lançamento."));
  }

  @Test
  @DisplayName("editar lançamento pago trocando conta e valor estorna a antiga e debita a nova")
  void editarLancamentoPago() throws Exception {
    UUID outraContaId = criarConta(usuarioId, "Carteira", "200.00");
    UUID id =
        idDe(
            criarLancamento(usuarioId, contaId, despesaId, "DESPESA", "100.00", null, HOJE)
                .andExpect(status().isCreated()));

    atualizar(
            URL + "/" + id,
            corpoLancamento(usuarioId, outraContaId, despesaId, "DESPESA", "30.00", null, HOJE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valor").value(30.00));

    assertThat(saldoAtual(contaId, usuarioId)).isEqualByComparingTo("1000.00");
    assertThat(saldoAtual(outraContaId, usuarioId)).isEqualByComparingTo("170.00");
  }

  @Test
  @DisplayName("cancelar lançamento pago estorna o saldo e trava edição e pagamento")
  void cancelarLancamentoPago() throws Exception {
    UUID id =
        idDe(
            criarLancamento(usuarioId, contaId, despesaId, "DESPESA", "100.00", null, HOJE)
                .andExpect(status().isCreated()));
    assertThat(saldoAtual(contaId, usuarioId)).isEqualByComparingTo("900.00");

    remover(URL + "/" + id, usuarioId).andExpect(status().isNoContent());
    assertThat(saldoAtual(contaId, usuarioId)).isEqualByComparingTo("1000.00");

    atualizar(
            URL + "/" + id,
            corpoLancamento(usuarioId, contaId, despesaId, "DESPESA", "100.00", null, HOJE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Não é possível editar um lançamento cancelado."));
    alterar(URL + "/" + id + "/pagar", corpoPagamento()).andExpect(status().isBadRequest());
    remover(URL + "/" + id, usuarioId).andExpect(status().isBadRequest());

    assertThat(saldoAtual(contaId, usuarioId)).isEqualByComparingTo("1000.00");
    consultar(URL + "/" + id, usuarioId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELADO"));
  }

  @Test
  @DisplayName("conta inativa não aceita novos lançamentos")
  void contaInativa() throws Exception {
    remover("/api/contas/" + contaId, usuarioId).andExpect(status().isNoContent());

    criarLancamento(usuarioId, contaId, despesaId, "DESPESA", "10.00", null, null)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("A conta informada está inativa."));
  }

  @Test
  @DisplayName("listagem filtra por status e devolve a página no formato estável")
  void listarComFiltros() throws Exception {
    criarLancamento(usuarioId, contaId, despesaId, "DESPESA", "10.00", null, HOJE)
        .andExpect(status().isCreated());
    criarLancamento(usuarioId, contaId, despesaId, "DESPESA", "20.00", HOJE.plusDays(3), null)
        .andExpect(status().isCreated());

    mockMvc
        .perform(get(URL).param("usuarioId", usuarioId.toString()).param("status", "PENDENTE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].valor").value(20.00))
        .andExpect(jsonPath("$.page.totalElements").value(1))
        .andExpect(jsonPath("$.page.number").value(0))
        .andExpect(jsonPath("$.pageable").doesNotExist());
  }

  @Test
  @DisplayName("período com data inicial depois da final é rejeitado")
  void periodoInvalido() throws Exception {
    mockMvc
        .perform(
            get(URL)
                .param("usuarioId", usuarioId.toString())
                .param("dataInicio", HOJE.toString())
                .param("dataFim", HOJE.minusDays(1).toString()))
        .andExpect(status().isBadRequest());
  }

  private String corpoPagamento() {
    return """
        {"usuarioId":"%s","dataPagamento":"%s"}
        """
        .formatted(usuarioId, HOJE);
  }
}
