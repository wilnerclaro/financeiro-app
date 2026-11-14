package br.com.financeiro.api.integracao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

@DisplayName("Dashboard financeiro")
class DashboardFinanceiroIntegracaoTest extends IntegracaoTest {

  @Test
  @DisplayName("resumo soma só os pagos e devolve o percentual como fração de 0 a 1")
  void resumoMensal() throws Exception {
    Sessao usuario = criarUsuario();
    UUID contaId = criarConta(usuario, "Banco", "0");
    UUID mercado = criarCategoria(usuario, "Mercado", "DESPESA", null);
    UUID lazer = criarCategoria(usuario, "Diversão", "DESPESA", null);
    UUID salario = criarCategoria(usuario, "Salário", "RECEITA", null);

    criarLancamento(usuario, contaId, mercado, "DESPESA", "75.00", null, HOJE)
        .andExpect(status().isCreated());
    criarLancamento(usuario, contaId, lazer, "DESPESA", "25.00", null, HOJE)
        .andExpect(status().isCreated());
    criarLancamento(usuario, contaId, salario, "RECEITA", "1000.00", null, HOJE)
        .andExpect(status().isCreated());
    criarLancamento(usuario, contaId, lazer, "DESPESA", "500.00", HOJE.plusDays(3), null)
        .andExpect(status().isCreated());

    resumo(usuario, HOJE.getYear(), HOJE.getMonthValue())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalReceitas").value(1000.00))
        .andExpect(jsonPath("$.totalDespesas").value(100.00))
        .andExpect(jsonPath("$.saldoPeriodo").value(900.00))
        .andExpect(jsonPath("$.quantidadeLancamentos").value(3))
        .andExpect(
            jsonPath("$.despesasPorCategoria[?(@.categoriaNome == 'Mercado')].percentual")
                .value(0.75))
        .andExpect(
            jsonPath("$.despesasPorCategoria[?(@.categoriaNome == 'Diversão')].percentual")
                .value(0.25))
        .andExpect(jsonPath("$.receitasPorCategoria[0].percentual").value(1.0));
  }

  @Test
  @DisplayName("mês sem lançamentos devolve zeros")
  void mesVazio() throws Exception {
    Sessao usuario = criarUsuario();

    resumo(usuario, 2025, 1)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalReceitas").value(0))
        .andExpect(jsonPath("$.receitasPorCategoria").isEmpty());
  }

  @Test
  @DisplayName("competência inválida é rejeitada")
  void validacoes() throws Exception {
    Sessao usuario = criarUsuario();

    resumo(usuario, 2026, 13).andExpect(status().isBadRequest());
    resumo(usuario, 1999, 1).andExpect(status().isBadRequest());
  }

  private ResultActions resumo(Sessao usuario, int ano, int mes) throws Exception {
    return mockMvc.perform(
        get("/api/dashboard-financeiro/resumo-mensal")
            .header(HttpHeaders.AUTHORIZATION, usuario.bearer())
            .param("ano", String.valueOf(ano))
            .param("mes", String.valueOf(mes)));
  }
}
