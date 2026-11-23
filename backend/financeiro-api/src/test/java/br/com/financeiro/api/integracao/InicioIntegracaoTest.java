package br.com.financeiro.api.integracao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Início e Independenciômetro")
class InicioIntegracaoTest extends IntegracaoTest {

  private static final String URL = "/api/dashboard-financeiro/inicio";
  private static final LocalDate MAIO = LocalDate.of(2026, 5, 10);

  private Sessao usuario;
  private UUID banco;
  private UUID salarios;
  private UUID alugueis;
  private UUID supermercado;

  @BeforeEach
  void prepararDados() throws Exception {
    usuario = criarUsuario();
    banco = criarConta(usuario, "Banco", "1000.00");
    salarios = categoriaPadrao(usuario, "Salários");
    alugueis = categoriaPadrao(usuario, "Aluguéis");
    supermercado = categoriaPadrao(usuario, "Supermercado");

    pagar(salarios, "RECEITA", "4000.00", MAIO);
    pagar(supermercado, "DESPESA", "2000.00", MAIO);

    pagar(salarios, "RECEITA", "4000.00", HOJE);
    pagar(alugueis, "RECEITA", "1000.00", HOJE);
    pagar(supermercado, "DESPESA", "3000.00", HOJE);
  }

  @Test
  @DisplayName("entrou, saiu, sobrou e as metas de 30% e 50%")
  void numerosDoMes() throws Exception {
    consultar(URL + "?ano=2026&mes=6", usuario)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.entrou").value(5000.00))
        .andExpect(jsonPath("$.saiu").value(3000.00))
        .andExpect(jsonPath("$.sobrou").value(2000.00))
        .andExpect(jsonPath("$.percentualGuardado").value(0.4))
        .andExpect(jsonPath("$.meta30.valor").value(1500.00))
        .andExpect(jsonPath("$.meta30.atingiu").value(true))
        .andExpect(jsonPath("$.meta30.falta").value(0.0))
        .andExpect(jsonPath("$.meta50.valor").value(2500.00))
        .andExpect(jsonPath("$.meta50.atingiu").value(false))
        .andExpect(jsonPath("$.meta50.falta").value(500.00))
        .andExpect(jsonPath("$.saldoEmContas").value(5000.00));
  }

  @Test
  @DisplayName("Independenciômetro = passiva / despesa, com faixa, próximo marco e quanto falta")
  void independenciometro() throws Exception {
    consultar(URL + "?ano=2026&mes=6", usuario)
        .andExpect(jsonPath("$.independenciometro.indice").value(0.3333))
        .andExpect(jsonPath("$.independenciometro.receitaPassiva").value(1000.00))
        .andExpect(jsonPath("$.independenciometro.despesaTotal").value(3000.00))
        .andExpect(jsonPath("$.independenciometro.indiceMesAnterior").value(0.0))
        .andExpect(jsonPath("$.independenciometro.variacao").value(0.3333))
        .andExpect(jsonPath("$.independenciometro.faixa").value(0.30))
        .andExpect(jsonPath("$.independenciometro.proximoMarco").value(0.50))
        .andExpect(jsonPath("$.independenciometro.faltaParaProximoMarco").value(500.00));
  }

  @Test
  @DisplayName("evolução de 6 meses com a variação contra o mês anterior")
  void evolucao() throws Exception {
    consultar(URL + "?ano=2026&mes=6", usuario)
        .andExpect(jsonPath("$.evolucao.length()").value(6))
        .andExpect(jsonPath("$.evolucao[0].mes").value(1))
        .andExpect(jsonPath("$.evolucao[0].evolucaoReceitas").value(0.0))
        .andExpect(jsonPath("$.evolucao[4].mes").value(5))
        .andExpect(jsonPath("$.evolucao[4].evolucaoReceitas").value(0.0))
        .andExpect(jsonPath("$.evolucao[5].evolucaoReceitas").value(0.25))
        .andExpect(jsonPath("$.evolucao[5].evolucaoDespesas").value(0.5))
        .andExpect(jsonPath("$.evolucao[5].independenciometro").value(0.3333));
  }

  @Test
  @DisplayName("uso do orçamento e categoria acima do previsto")
  void orcamentoEAlertas() throws Exception {
    atualizar(
            usuario,
            "/api/orcamentos/2026/6",
            """
            {"itens":[{"categoriaId":"%s","valorPrevisto":3200.00},
                      {"categoriaId":"%s","valorPrevisto":4000.00}]}
            """
                .formatted(supermercado, salarios))
        .andExpect(status().isOk());

    consultar(URL + "?ano=2026&mes=6", usuario)
        .andExpect(jsonPath("$.orcamento.despesasPrevistas").value(3200.00))
        .andExpect(jsonPath("$.orcamento.receitasPrevistas").value(4000.00))
        .andExpect(jsonPath("$.orcamento.percentualDespesasUsado").value(0.9375))
        .andExpect(jsonPath("$.orcamento.restaDasDespesas").value(200.00))
        .andExpect(jsonPath("$.categoriasAcimaDoPrevisto.length()").value(1))
        .andExpect(jsonPath("$.categoriasAcimaDoPrevisto[0].nome").value("Supermercado"))
        .andExpect(jsonPath("$.categoriasAcimaDoPrevisto[0].grupo").value("Alimentação"))
        .andExpect(jsonPath("$.categoriasAcimaDoPrevisto[0].percentualRealizado").value(0.9375))
        .andExpect(jsonPath("$.categoriasAcimaDoPrevisto[0].desvio").value(-200.00));
  }

  @Test
  @DisplayName("para pagar soma os pendentes até o fim do mês, atrasado primeiro")
  void paraPagar() throws Exception {
    criarLancamento(usuario, banco, supermercado, "DESPESA", "80.00", HOJE.plusDays(5), null);
    criarLancamento(usuario, banco, supermercado, "DESPESA", "40.00", HOJE.minusDays(2), null);
    criarLancamento(usuario, banco, supermercado, "DESPESA", "999.00", HOJE.plusMonths(1), null);

    consultar(URL, usuario)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ano").value(2026))
        .andExpect(jsonPath("$.mes").value(6))
        .andExpect(jsonPath("$.paraPagar.total").value(120.00))
        .andExpect(jsonPath("$.paraPagar.quantidade").value(2))
        .andExpect(jsonPath("$.paraPagar.itens[0].status").value("ATRASADO"))
        .andExpect(jsonPath("$.paraPagar.itens[0].valor").value(40.00))
        .andExpect(jsonPath("$.paraPagar.itens[1].status").value("PENDENTE"))
        .andExpect(jsonPath("$.saiu").value(3000.00));
  }

  @Test
  @DisplayName("comparativo com o mês anterior, últimos lançamentos e lançamento rápido")
  void destaquesEAtalhos() throws Exception {
    consultar(URL + "?ano=2026&mes=6", usuario)
        .andExpect(jsonPath("$.comparativoMesAnterior.diferencaDespesas").value(1000.00))
        .andExpect(jsonPath("$.comparativoMesAnterior.evolucaoDespesas").value(0.5))
        .andExpect(jsonPath("$.comparativoMesAnterior.diferencaReceitas").value(1000.00))
        .andExpect(jsonPath("$.evolucao[5].percentualGuardado").value(0.4))
        .andExpect(jsonPath("$.evolucao[5].atingiuMeta30").value(true))
        .andExpect(jsonPath("$.evolucao[4].percentualGuardado").value(0.5))
        .andExpect(jsonPath("$.evolucao[3].atingiuMeta30").value(false))
        .andExpect(jsonPath("$.ultimosLancamentos.length()").value(5))
        .andExpect(jsonPath("$.ultimosLancamentos[0].dataCompetencia").value("2026-06-15"))
        .andExpect(jsonPath("$.lancamentoRapido.contaId").value(banco.toString()))
        .andExpect(jsonPath("$.lancamentoRapido.nomeConta").value("Banco"))
        .andExpect(
            jsonPath("$.lancamentoRapido.categoriasMaisUsadas[0].nome").value("Supermercado"))
        .andExpect(jsonPath("$.lancamentoRapido.categoriasMaisUsadas[0].quantidade").value(2));
  }

  @Test
  @DisplayName("receita acima do previsto e grupos do orçamento mais usados")
  void receitaAcimaEGrupos() throws Exception {
    atualizar(
            usuario,
            "/api/orcamentos/2026/6",
            """
            {"itens":[{"categoriaId":"%s","valorPrevisto":800.00},
                      {"categoriaId":"%s","valorPrevisto":3200.00}]}
            """
                .formatted(alugueis, supermercado))
        .andExpect(status().isOk());

    consultar(URL + "?ano=2026&mes=6", usuario)
        .andExpect(jsonPath("$.receitasAcimaDoPrevisto.length()").value(1))
        .andExpect(jsonPath("$.receitasAcimaDoPrevisto[0].nome").value("Aluguéis"))
        .andExpect(jsonPath("$.receitasAcimaDoPrevisto[0].desvio").value(200.00))
        .andExpect(jsonPath("$.orcamento.gruposMaisUsados.length()").value(1))
        .andExpect(jsonPath("$.orcamento.gruposMaisUsados[0].nome").value("Alimentação"))
        .andExpect(jsonPath("$.orcamento.gruposMaisUsados[0].desvio").value(-200.00));
  }

  @Test
  @DisplayName("mês sem movimento devolve zeros, nunca erro; ano sem mês é recusado")
  void mesVazioEValidacao() throws Exception {
    consultar(URL + "?ano=2025&mes=1", usuario)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.entrou").value(0.0))
        .andExpect(jsonPath("$.percentualGuardado").value(0.0))
        .andExpect(jsonPath("$.independenciometro.indice").value(0.0))
        .andExpect(jsonPath("$.independenciometro.proximoMarco").value(0.10))
        .andExpect(jsonPath("$.meta30.atingiu").value(false));

    mockMvc
        .perform(autenticado(get(URL), usuario).param("ano", "2026"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("os números de um usuário não aparecem para outro")
  void isolamento() throws Exception {
    Sessao outro = criarUsuario();
    consultar(URL + "?ano=2026&mes=6", outro)
        .andExpect(jsonPath("$.entrou").value(0.0))
        .andExpect(jsonPath("$.saldoEmContas").value(0.0))
        .andExpect(jsonPath("$.paraPagar.quantidade").value(0))
        .andExpect(jsonPath("$.lancamentoRapido.contaId").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.lancamentoRapido.categoriasMaisUsadas.length()").value(0));
  }

  private void pagar(UUID categoria, String tipo, String valor, LocalDate data) throws Exception {
    criarLancamentoEm(usuario, banco, categoria, tipo, valor, data, null, data)
        .andExpect(status().isCreated());
  }
}
