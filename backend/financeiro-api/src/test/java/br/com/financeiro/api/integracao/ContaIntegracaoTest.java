package br.com.financeiro.api.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

@DisplayName("Contas")
class ContaIntegracaoTest extends IntegracaoTest {

  private static final String URL = "/api/contas";

  private Sessao usuario;

  @BeforeEach
  void prepararDados() throws Exception {
    usuario = criarUsuario();
  }

  @Test
  @DisplayName("criação devolve nome como digitado, saldo atual igual ao inicial e datas")
  void criarConta() throws Exception {
    postar(
            usuario,
            URL,
            """
            {"nome":" Nubank  Roxinho ","tipo":"CONTA_CORRENTE","saldoInicial":250.50}
            """)
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
    criarConta(usuario, "Itaú", "0");

    postar(
            usuario,
            URL,
            """
            {"nome":"ITAÚ","tipo":"POUPANCA","saldoInicial":0}
            """)
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").value("Já existe uma conta com esse nome para este usuário."));
  }

  @Test
  @DisplayName("PUT altera nome e tipo sem mexer no status")
  void atualizarConta() throws Exception {
    UUID id = criarConta(usuario, "Carteira", "50");
    remover(URL + "/" + id, usuario).andExpect(status().isNoContent());

    atualizar(
            usuario,
            URL + "/" + id,
            """
            {"nome":"Dinheiro vivo","tipo":"DINHEIRO"}
            """)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Dinheiro vivo"))
        .andExpect(jsonPath("$.tipo").value("DINHEIRO"))
        .andExpect(jsonPath("$.ativa").value(false));
  }

  @Test
  @DisplayName("inativar e ativar são idempotentes")
  void inativarEAtivar() throws Exception {
    UUID id = criarConta(usuario, "Poupança", "0");

    remover(URL + "/" + id, usuario).andExpect(status().isNoContent());
    remover(URL + "/" + id, usuario).andExpect(status().isNoContent());
    consultar(URL + "/" + id, usuario).andExpect(jsonPath("$.ativa").value(false));

    ativar(URL + "/" + id + "/ativar", usuario)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ativa").value(true));
    ativar(URL + "/" + id + "/ativar", usuario).andExpect(status().isOk());
  }

  @Test
  @DisplayName("corrigir saldo inicial recalcula o saldo atual considerando os pagos")
  void corrigirSaldoInicial() throws Exception {
    UUID contaId = criarConta(usuario, "Banco", "100.00");
    UUID despesa = criarCategoria(usuario, "Contas", "DESPESA", null);
    UUID receita = criarCategoria(usuario, "Freela", "RECEITA", null);
    criarLancamento(usuario, contaId, despesa, "DESPESA", "40.00", null, HOJE)
        .andExpect(status().isCreated());
    criarLancamento(usuario, contaId, receita, "RECEITA", "300.00", null, HOJE)
        .andExpect(status().isCreated());
    criarLancamento(usuario, contaId, despesa, "DESPESA", "999.00", HOJE.plusDays(2), null)
        .andExpect(status().isCreated());

    alterar(
            usuario,
            URL + "/" + contaId + "/corrigir-saldo-inicial",
            """
            {"saldoInicial":1000.00}
            """)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.saldoInicial").value(1000.00));

    assertThat(saldoAtual(contaId, usuario)).isEqualByComparingTo("1260.00");
  }

  @Test
  @DisplayName("listagem filtra por status")
  void listarPorStatus() throws Exception {
    criarConta(usuario, "Ativa", "0");
    UUID inativa = criarConta(usuario, "Inativa", "0");
    remover(URL + "/" + inativa, usuario).andExpect(status().isNoContent());

    mockMvc
        .perform(
            get(URL).header(HttpHeaders.AUTHORIZATION, usuario.bearer()).param("ativa", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].nome").value("Inativa"));
  }

  @Test
  @DisplayName("conta inexistente devolve 404")
  void contaInexistente() throws Exception {
    consultar(URL + "/" + UUID.randomUUID(), usuario).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("resumo soma o patrimônio das contas ativas por tipo, com o cartão negativo")
  void resumoDasContas() throws Exception {
    Sessao dono = criarUsuario();
    criarConta(dono, "Banco", "1000.00");
    criarConta(dono, "Outro banco", "500.00");
    UUID cartao = criarCartao(dono, "Cartão", 10, 20);
    UUID inativa = criarConta(dono, "Antiga", "300.00");
    remover("/api/contas/" + inativa, dono);
    UUID categoria = categoriaPadrao(dono, "Supermercado");
    criarLancamentoEm(dono, cartao, categoria, "DESPESA", "200.00", HOJE, null, HOJE);

    consultar("/api/contas/resumo", dono)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.patrimonio").value(1300.00))
        .andExpect(jsonPath("$.porTipo[0].tipo").value("CONTA_CORRENTE"))
        .andExpect(jsonPath("$.porTipo[0].total").value(1500.00))
        .andExpect(jsonPath("$.porTipo[0].quantidadeDeContas").value(2))
        .andExpect(jsonPath("$.porTipo[1].tipo").value("CARTAO_CREDITO"))
        .andExpect(jsonPath("$.porTipo[1].total").value(-200.00));

    consultar("/api/contas/" + cartao, dono)
        .andExpect(jsonPath("$.variacaoDesdeSaldoInicial").value(-200.00));
  }
}
