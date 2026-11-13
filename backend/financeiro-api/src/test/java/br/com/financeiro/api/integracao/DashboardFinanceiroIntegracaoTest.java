package br.com.financeiro.api.integracao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

@DisplayName("Dashboard financeiro")
class DashboardFinanceiroIntegracaoTest extends IntegracaoTest {

  @Test
  @DisplayName("resumo soma só os pagos e devolve o percentual como fração de 0 a 1")
  void resumoMensal() throws Exception {
    UUID usuarioId = criarUsuario();
    UUID contaId = criarConta(usuarioId, "Banco", "0");
    UUID mercado = criarCategoria(usuarioId, "Mercado", "DESPESA", null);
    UUID lazer = criarCategoria(usuarioId, "Lazer", "DESPESA", null);
    UUID salario = criarCategoria(usuarioId, "Salário", "RECEITA", null);

    criarLancamento(usuarioId, contaId, mercado, "DESPESA", "75.00", null, HOJE)
        .andExpect(status().isCreated());
    criarLancamento(usuarioId, contaId, lazer, "DESPESA", "25.00", null, HOJE)
        .andExpect(status().isCreated());
    criarLancamento(usuarioId, contaId, salario, "RECEITA", "1000.00", null, HOJE)
        .andExpect(status().isCreated());
    criarLancamento(usuarioId, contaId, lazer, "DESPESA", "500.00", HOJE.plusDays(3), null)
        .andExpect(status().isCreated());

    resumo(usuarioId, HOJE.getYear(), HOJE.getMonthValue())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalReceitas").value(1000.00))
        .andExpect(jsonPath("$.totalDespesas").value(100.00))
        .andExpect(jsonPath("$.saldoPeriodo").value(900.00))
        .andExpect(jsonPath("$.quantidadeLancamentos").value(3))
        .andExpect(
            jsonPath("$.despesasPorCategoria[?(@.categoriaNome == 'Mercado')].percentual")
                .value(0.75))
        .andExpect(
            jsonPath("$.despesasPorCategoria[?(@.categoriaNome == 'Lazer')].percentual")
                .value(0.25))
        .andExpect(jsonPath("$.receitasPorCategoria[0].percentual").value(1.0));
  }

  @Test
  @DisplayName("mês sem lançamentos devolve zeros")
  void mesVazio() throws Exception {
    UUID usuarioId = criarUsuario();

    resumo(usuarioId, 2025, 1)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalReceitas").value(0))
        .andExpect(jsonPath("$.receitasPorCategoria").isEmpty());
  }

  @Test
  @DisplayName("competência inválida é rejeitada e usuário inexistente devolve 404")
  void validacoes() throws Exception {
    UUID usuarioId = criarUsuario();

    resumo(usuarioId, 2026, 13).andExpect(status().isBadRequest());
    resumo(usuarioId, 1999, 1).andExpect(status().isBadRequest());
    resumo(UUID.randomUUID(), 2026, 1).andExpect(status().isNotFound());
  }

  private ResultActions resumo(UUID usuarioId, int ano, int mes) throws Exception {
    return mockMvc.perform(
        get("/api/dashboard-financeiro/resumo-mensal")
            .param("usuarioId", usuarioId.toString())
            .param("ano", String.valueOf(ano))
            .param("mes", String.valueOf(mes)));
  }
}
