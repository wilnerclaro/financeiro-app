package br.com.financeiro.api.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Contas")
class ContaIntegracaoTest extends IntegracaoTest {

  private static final String URL = "/api/contas";

  private UUID usuarioId;

  @BeforeEach
  void prepararDados() {
    usuarioId = criarUsuario();
  }

  @Test
  @DisplayName("criação devolve nome como digitado, saldo atual igual ao inicial e datas")
  void criarConta() throws Exception {
    postar(
            URL,
            """
            {"usuarioId":"%s","nome":" Nubank  Roxinho ","tipo":"CONTA_CORRENTE","saldoInicial":250.50}
            """
                .formatted(usuarioId))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nome").value("Nubank Roxinho"))
        .andExpect(jsonPath("$.saldoInicial").value(250.50))
        .andExpect(jsonPath("$.saldoAtual").value(250.50))
        .andExpect(jsonPath("$.ativa").value(true))
        .andExpect(jsonPath("$.criadoEm").isNotEmpty());
  }

  @Test
  @DisplayName("nome duplicado é rejeitado sem diferenciar maiúsculas")
  void nomeDuplicado() throws Exception {
    criarConta(usuarioId, "Itaú", "0");

    postar(
            URL,
            """
            {"usuarioId":"%s","nome":"ITAÚ","tipo":"POUPANCA","saldoInicial":0}
            """
                .formatted(usuarioId))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").value("Já existe uma conta com esse nome para este usuário."));
  }

  @Test
  @DisplayName("PUT altera nome e tipo sem mexer no status")
  void atualizarConta() throws Exception {
    UUID id = criarConta(usuarioId, "Carteira", "50");
    remover(URL + "/" + id, usuarioId).andExpect(status().isNoContent());

    atualizar(
            URL + "/" + id,
            """
            {"usuarioId":"%s","nome":"Dinheiro vivo","tipo":"DINHEIRO"}
            """
                .formatted(usuarioId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Dinheiro vivo"))
        .andExpect(jsonPath("$.tipo").value("DINHEIRO"))
        .andExpect(jsonPath("$.ativa").value(false));
  }

  @Test
  @DisplayName("inativar e ativar são idempotentes")
  void inativarEAtivar() throws Exception {
    UUID id = criarConta(usuarioId, "Poupança", "0");

    remover(URL + "/" + id, usuarioId).andExpect(status().isNoContent());
    remover(URL + "/" + id, usuarioId).andExpect(status().isNoContent());
    consultar(URL + "/" + id, usuarioId).andExpect(jsonPath("$.ativa").value(false));

    ativar(URL + "/" + id + "/ativar", usuarioId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ativa").value(true));
    ativar(URL + "/" + id + "/ativar", usuarioId).andExpect(status().isOk());
  }

  @Test
  @DisplayName("corrigir saldo inicial recalcula o saldo atual considerando os pagos")
  void corrigirSaldoInicial() throws Exception {
    UUID contaId = criarConta(usuarioId, "Banco", "100.00");
    UUID despesa = criarCategoria(usuarioId, "Contas", "DESPESA", null);
    UUID receita = criarCategoria(usuarioId, "Freela", "RECEITA", null);
    criarLancamento(usuarioId, contaId, despesa, "DESPESA", "40.00", null, HOJE)
        .andExpect(status().isCreated());
    criarLancamento(usuarioId, contaId, receita, "RECEITA", "300.00", null, HOJE)
        .andExpect(status().isCreated());
    criarLancamento(usuarioId, contaId, despesa, "DESPESA", "999.00", HOJE.plusDays(2), null)
        .andExpect(status().isCreated());

    alterar(
            URL + "/" + contaId + "/corrigir-saldo-inicial",
            """
            {"usuarioId":"%s","saldoInicial":1000.00}
            """
                .formatted(usuarioId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.saldoInicial").value(1000.00));

    assertThat(saldoAtual(contaId, usuarioId)).isEqualByComparingTo("1260.00");
  }

  @Test
  @DisplayName("listagem filtra por status")
  void listarPorStatus() throws Exception {
    criarConta(usuarioId, "Ativa", "0");
    UUID inativa = criarConta(usuarioId, "Inativa", "0");
    remover(URL + "/" + inativa, usuarioId).andExpect(status().isNoContent());

    mockMvc
        .perform(get(URL).param("usuarioId", usuarioId.toString()).param("ativa", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].nome").value("Inativa"));
  }

  @Test
  @DisplayName("conta inexistente devolve 404")
  void contaInexistente() throws Exception {
    consultar(URL + "/" + UUID.randomUUID(), usuarioId).andExpect(status().isNotFound());
  }
}
