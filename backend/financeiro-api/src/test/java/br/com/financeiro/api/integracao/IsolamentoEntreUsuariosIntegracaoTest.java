package br.com.financeiro.api.integracao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

@DisplayName("Isolamento entre usuários")
class IsolamentoEntreUsuariosIntegracaoTest extends IntegracaoTest {

  private Sessao usuarioA;
  private Sessao usuarioB;
  private UUID contaA;
  private UUID categoriaA;
  private UUID lancamentoA;

  @BeforeEach
  void prepararDados() throws Exception {
    usuarioA = criarUsuario();
    usuarioB = criarUsuario();
    contaA = criarConta(usuarioA, "Conta do A", "100.00");
    categoriaA = criarCategoria(usuarioA, "Diversão", "DESPESA", null);
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
  @DisplayName("as listagens de B vêm vazias (categorias: só as padrão do próprio B)")
  void listagensVazias() throws Exception {
    for (String url :
        new String[] {"/api/contas", "/api/lancamentos-financeiros", "/api/transferencias"}) {
      mockMvc
          .perform(get(url).header(HttpHeaders.AUTHORIZATION, usuarioB.bearer()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.page.totalElements").value(0));
    }
    mockMvc
        .perform(
            get("/api/categorias")
                .header(HttpHeaders.AUTHORIZATION, usuarioB.bearer())
                .param("size", "200"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements").value(100))
        .andExpect(jsonPath("$.content[?(@.id == '" + categoriaA + "')]").isEmpty());
    mockMvc
        .perform(get("/api/metas").header(HttpHeaders.AUTHORIZATION, usuarioB.bearer()))
        .andExpect(jsonPath("$.metas").isEmpty());
  }

  @Test
  @DisplayName("B não lança na conta de A nem usa a categoria de A")
  void naoUsaRecursosDeOutro() throws Exception {
    UUID contaB = criarConta(usuarioB, "Conta do B", "0.00");
    UUID categoriaB = criarCategoria(usuarioB, "Diversão", "DESPESA", null);

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
            usuarioB,
            "/api/contas/" + contaA,
            """
            {"nome":"Invasão","tipo":"POUPANCA"}
            """)
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
                .header(HttpHeaders.AUTHORIZATION, usuarioB.bearer())
                .param("ano", String.valueOf(HOJE.getYear()))
                .param("mes", String.valueOf(HOJE.getMonthValue())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalDespesas").value(0))
        .andExpect(jsonPath("$.quantidadeLancamentos").value(0));
  }

  @Test
  @DisplayName("um usuarioId enviado no corpo é ignorado: vale sempre o dono do token")
  void usuarioIdNoCorpoEhIgnorado() throws Exception {
    UUID conta =
        idDe(
            postar(
                    usuarioB,
                    "/api/contas",
                    """
                    {"usuarioId":"%s","nome":"Tentativa","tipo":"POUPANCA","saldoInicial":0}
                    """
                        .formatted(usuarioA.id()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(usuarioB.id().toString())));

    consultar("/api/contas/" + conta, usuarioA).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("B não vê nem cancela a transferência de A")
  void transferenciaIsolada() throws Exception {
    UUID poupancaA = criarConta(usuarioA, "Poupança do A", "0.00");
    UUID transferencia =
        idDe(
            postar(
                    usuarioA,
                    "/api/transferencias",
                    """
                    {"contaOrigemId":"%s","contaDestinoId":"%s","valor":10.00,
                     "dataTransferencia":"%s"}
                    """
                        .formatted(contaA, poupancaA, HOJE))
                .andExpect(status().isCreated()));

    consultar("/api/transferencias/" + transferencia, usuarioB).andExpect(status().isNotFound());
    remover("/api/transferencias/" + transferencia, usuarioB).andExpect(status().isNotFound());
    consultar("/api/transferencias/" + transferencia, usuarioA)
        .andExpect(jsonPath("$.cancelada").value(false));
  }

  @Test
  @DisplayName("B não vê, edita, inativa nem lança aporte na meta de A")
  void metaIsolada() throws Exception {
    String corpo =
        """
        {"nome":"Reserva","valorNecessario":1000.00,"investimentoInicial":0}
        """;
    UUID meta = idDe(postar(usuarioA, "/api/metas", corpo).andExpect(status().isCreated()));
    String url = "/api/metas/" + meta;

    consultar(url, usuarioB).andExpect(status().isNotFound());
    atualizar(usuarioB, url, corpo).andExpect(status().isNotFound());
    remover(url, usuarioB).andExpect(status().isNotFound());
    ativar(url + "/ativar", usuarioB).andExpect(status().isNotFound());
    atualizar(usuarioB, url + "/movimentos/2026/6", "{\"aporte\":10.00}")
        .andExpect(status().isNotFound());

    consultar(url, usuarioA)
        .andExpect(jsonPath("$.ativa").value(true))
        .andExpect(jsonPath("$.acumulado").value(0.0));
  }

  @Test
  @DisplayName("B não vê nem usa o previsto de A")
  void orcamentoIsolado() throws Exception {
    String previsto =
        """
        {"itens":[{"categoriaId":"%s","valorPrevisto":300.00}]}
        """
            .formatted(categoriaA);
    atualizar(usuarioA, "/api/orcamentos/2026/6", previsto).andExpect(status().isOk());

    atualizar(usuarioB, "/api/orcamentos/2026/6", previsto).andExpect(status().isNotFound());
    postar(
            usuarioB,
            "/api/orcamentos/repetir",
            """
            {"categoriaId":"%s","valorPrevisto":10.00,"substituirExistentes":true,
             "meses":["2026-07"]}
            """
                .formatted(categoriaA))
        .andExpect(status().isNotFound());
    consultar("/api/orcamentos/2026/6", usuarioB)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.despesas.total.previsto").value(0.0));
    consultar("/api/orcamentos/2026", usuarioB)
        .andExpect(jsonPath("$.linhas[?(@.categoriaId == '" + categoriaA + "')]").isEmpty());
  }

  @Test
  @DisplayName("Início, resumo das contas e fatura de B não usam dados de A")
  void paineisIsolados() throws Exception {
    UUID cartaoA = criarCartao(usuarioA, "Cartão do A", 5, 15);

    consultar("/api/dashboard-financeiro/inicio?ano=2026&mes=6", usuarioB)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.saiu").value(0.0))
        .andExpect(jsonPath("$.ultimosLancamentos").isEmpty())
        .andExpect(jsonPath("$.saldoEmContas").value(0.0));
    consultar("/api/contas/resumo", usuarioB)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.patrimonio").value(0.0))
        .andExpect(jsonPath("$.porTipo").isEmpty());
    consultar(
            "/api/lancamentos-financeiros/faturas?contaId=" + cartaoA + "&ano=2026&mes=6", usuarioB)
        .andExpect(status().isNotFound());
  }
}
