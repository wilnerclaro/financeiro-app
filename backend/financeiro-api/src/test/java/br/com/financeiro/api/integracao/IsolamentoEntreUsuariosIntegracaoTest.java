package br.com.financeiro.api.integracao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Garante que um usuário nunca enxerga nem altera dados de outro. Hoje o usuário vem no request;
 * quando o JWT chegar, estes testes continuam valendo com o usuário vindo do token.
 */
@DisplayName("Isolamento entre usuários")
class IsolamentoEntreUsuariosIntegracaoTest extends IntegracaoTest {

  private UUID usuarioA;
  private UUID usuarioB;
  private UUID contaA;
  private UUID categoriaA;
  private UUID lancamentoA;

  @BeforeEach
  void prepararDados() throws Exception {
    usuarioA = criarUsuario();
    usuarioB = criarUsuario();
    contaA = criarConta(usuarioA, "Conta do A", "100.00");
    categoriaA = criarCategoria(usuarioA, "Lazer", "DESPESA", null);
    lancamentoA =
        idDe(
            criarLancamento(usuarioA, contaA, categoriaA, "DESPESA", "10.00", null, HOJE)
                .andExpect(status().isCreated()));
  }

  @Test
  @DisplayName("B não encontra a conta, a categoria nem o lançamento de A")
  void naoEnxergaPorId() throws Exception {
    consultar("/api/contas/" + contaA, usuarioB).andExpect(status().isNotFound());
    consultar("/api/categorias/" + categoriaA, usuarioB).andExpect(status().isNotFound());
    consultar("/api/lancamentos-financeiros/" + lancamentoA, usuarioB)
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("as listagens de B vêm vazias")
  void listagensVazias() throws Exception {
    for (String url :
        new String[] {"/api/contas", "/api/categorias", "/api/lancamentos-financeiros"}) {
      mockMvc
          .perform(get(url).param("usuarioId", usuarioB.toString()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.page.totalElements").value(0));
    }
  }

  @Test
  @DisplayName("B não lança na conta de A nem usa a categoria de A")
  void naoUsaRecursosDeOutro() throws Exception {
    UUID contaB = criarConta(usuarioB, "Conta do B", "0.00");
    UUID categoriaB = criarCategoria(usuarioB, "Lazer", "DESPESA", null);

    criarLancamento(usuarioB, contaA, categoriaB, "DESPESA", "5.00", null, null)
        .andExpect(status().isNotFound());
    criarLancamento(usuarioB, contaB, categoriaA, "DESPESA", "5.00", null, null)
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("B não inativa, edita nem cancela registros de A")
  void naoAlteraRecursosDeOutro() throws Exception {
    remover("/api/contas/" + contaA, usuarioB).andExpect(status().isNotFound());
    remover("/api/categorias/" + categoriaA, usuarioB).andExpect(status().isNotFound());
    remover("/api/lancamentos-financeiros/" + lancamentoA, usuarioB)
        .andExpect(status().isNotFound());
    atualizar(
            "/api/contas/" + contaA,
            """
            {"usuarioId":"%s","nome":"Invasão","tipo":"POUPANCA"}
            """
                .formatted(usuarioB))
        .andExpect(status().isNotFound());

    consultar("/api/contas/" + contaA, usuarioA)
        .andExpect(jsonPath("$.nome").value("Conta do A"))
        .andExpect(jsonPath("$.ativa").value(true))
        .andExpect(jsonPath("$.saldoAtual").value(90.00));
  }

  @Test
  @DisplayName("o resumo mensal de B não soma os lançamentos de A")
  void dashboardIsolado() throws Exception {
    mockMvc
        .perform(
            get("/api/dashboard-financeiro/resumo-mensal")
                .param("usuarioId", usuarioB.toString())
                .param("ano", String.valueOf(HOJE.getYear()))
                .param("mes", String.valueOf(HOJE.getMonthValue())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalDespesas").value(0))
        .andExpect(jsonPath("$.quantidadeLancamentos").value(0));
  }
}
