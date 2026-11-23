package br.com.financeiro.api.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financeiro.api.configuracaoinicial.repository.ConfiguracaoInicialRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class ConfiguracaoInicialIntegracaoTest extends IntegracaoTest {

  private static final String URL = "/api/configuracao-inicial";

  @Autowired private ConfiguracaoInicialRepository repository;

  @Test
  void leituraInicialUsaRelogioSemCriarRegistro() throws Exception {
    Sessao usuario = criarUsuario();
    consultar(URL, usuario)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rendaPassivaDesejada").doesNotExist())
        .andExpect(jsonPath("$.etapa").value("BOAS_VINDAS"))
        .andExpect(jsonPath("$.pausado").value(false))
        .andExpect(jsonPath("$.ano").value(HOJE.getYear()))
        .andExpect(jsonPath("$.mes").value(HOJE.getMonthValue()));
    assertThat(repository.findByUsuarioId(usuario.id())).isEmpty();
  }

  @Test
  void pausaERetomaSemPerderObjetivoOuCompetencia() throws Exception {
    Sessao usuario = criarUsuario();
    atualizar(usuario, URL, pedido("6000.50", "ORCAMENTO", true, 2027, 3))
        .andExpect(status().isOk());
    consultar(URL, usuario)
        .andExpect(jsonPath("$.rendaPassivaDesejada").value(6000.5))
        .andExpect(jsonPath("$.etapa").value("ORCAMENTO"))
        .andExpect(jsonPath("$.pausado").value(true))
        .andExpect(jsonPath("$.ano").value(2027))
        .andExpect(jsonPath("$.mes").value(3));
    UUID id = repository.findByUsuarioId(usuario.id()).orElseThrow().getId();
    atualizar(usuario, URL, pedido("6000.50", "ORCAMENTO", false, 2027, 3))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pausado").value(false));
    assertThat(repository.findByUsuarioId(usuario.id()).orElseThrow().getId()).isEqualTo(id);
  }

  @Test
  void objetivosSaoIsoladosPorUsuario() throws Exception {
    Sessao primeiro = criarUsuario();
    Sessao segundo = criarUsuario();
    atualizar(primeiro, URL, pedido("5000", "ORCAMENTO", false, 2026, 6))
        .andExpect(status().isOk());
    consultar(URL, segundo).andExpect(jsonPath("$.etapa").value("BOAS_VINDAS"));
    atualizar(segundo, URL, pedido("8000", "CONTAS", true, 2026, 7)).andExpect(status().isOk());
    consultar(URL, primeiro)
        .andExpect(jsonPath("$.rendaPassivaDesejada").value(5000))
        .andExpect(jsonPath("$.etapa").value("ORCAMENTO"))
        .andExpect(jsonPath("$.pausado").value(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"BOAS_VINDAS", "OBJETIVO"})
  void permitePausarAntesDeDefinirObjetivo(String etapa) throws Exception {
    atualizar(criarUsuario(), URL, pedido("null", etapa, true, 2026, 6))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rendaPassivaDesejada").doesNotExist());
  }

  @ParameterizedTest
  @ValueSource(strings = {"ORCAMENTO", "CONTAS", "LANCAMENTO", "CONCLUIDO"})
  void naoAvancaSemObjetivo(String etapa) throws Exception {
    atualizar(criarUsuario(), URL, pedido("null", etapa, false, 2026, 6))
        .andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "1.001", "100000000000000000"})
  void recusaObjetivoInvalido(String valor) throws Exception {
    atualizar(criarUsuario(), URL, pedido(valor, "OBJETIVO", false, 2026, 6))
        .andExpect(status().isBadRequest());
  }

  @Test
  void validaCompetenciaECamposObrigatorios() throws Exception {
    Sessao usuario = criarUsuario();
    atualizar(usuario, URL, pedido("1", "OBJETIVO", false, 1999, 6))
        .andExpect(status().isBadRequest());
    atualizar(usuario, URL, pedido("1", "OBJETIVO", false, 2026, 13))
        .andExpect(status().isBadRequest());
    atualizar(usuario, URL, "{}").andExpect(status().isBadRequest());
    atualizar(usuario, URL, pedido("1", "INEXISTENTE", false, 2026, 6))
        .andExpect(status().isBadRequest());
  }

  @Test
  void concluirNaoCriaLancamentoNemAlteraSaldoOuIndicador() throws Exception {
    Sessao usuario = criarUsuario();
    UUID conta = criarConta(usuario, "Minha conta", "5000");
    String antes =
        consultar("/api/dashboard-financeiro/inicio", usuario)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    atualizar(usuario, URL, pedido("6000", "CONCLUIDO", false, 2026, 6))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.etapa").value("CONCLUIDO"));
    assertThat(saldoAtual(conta, usuario)).isEqualByComparingTo(new BigDecimal("5000"));
    consultar("/api/lancamentos-financeiros", usuario)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements").value(0));
    String depois =
        consultar("/api/dashboard-financeiro/inicio", usuario)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(depois).isEqualTo(antes);
  }

  @Test
  void exigeAutenticacao() throws Exception {
    mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
  }

  private static String pedido(String renda, String etapa, boolean pausado, int ano, int mes) {
    return """
        {"rendaPassivaDesejada":%s,"etapa":"%s","pausado":%s,"ano":%d,"mes":%d}
        """
        .formatted(renda, etapa, pausado, ano, mes);
  }
}
