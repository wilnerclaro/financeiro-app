package br.com.financeiro.api.integracao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financeiro.api.TestcontainersConfiguration;
import br.com.financeiro.api.usuario.entity.Usuario;
import br.com.financeiro.api.usuario.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Base dos testes de integração: sobe a aplicação inteira contra um PostgreSQL real
 * (Testcontainers) e conversa com ela por HTTP via MockMvc.
 *
 * <p>Cada teste cria o próprio usuário, então os testes não dependem de ordem nem de limpeza do
 * banco. O relógio é fixo em {@link #HOJE} para que regras de data sejam determinísticas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, IntegracaoTest.RelogioFixo.class})
abstract class IntegracaoTest {

  static final LocalDate HOJE = LocalDate.of(2026, 6, 15);

  @Autowired protected MockMvc mockMvc;

  @Autowired private UsuarioRepository usuarioRepository;

  @TestConfiguration(proxyBeanMethods = false)
  static class RelogioFixo {

    @Bean
    @Primary
    Clock relogioFixo() {
      ZoneId fuso = ZoneId.of("America/Sao_Paulo");
      return Clock.fixed(HOJE.atStartOfDay(fuso).plusHours(12).toInstant(), fuso);
    }
  }

  protected UUID criarUsuario() {
    Usuario usuario = new Usuario();
    usuario.setNome("Usuário de teste");
    usuario.setEmail(UUID.randomUUID() + "@teste.local");
    usuario.setSenhaHash("nao-usado");
    usuario.setAtivo(true);
    return usuarioRepository.saveAndFlush(usuario).getId();
  }

  protected UUID criarConta(UUID usuarioId, String nome, String saldoInicial) throws Exception {
    String corpo =
        """
        {"usuarioId":"%s","nome":"%s","tipo":"CONTA_CORRENTE","saldoInicial":%s}
        """
            .formatted(usuarioId, nome, saldoInicial);
    return idDe(postar("/api/contas", corpo).andExpect(status().isCreated()));
  }

  protected UUID criarCategoria(UUID usuarioId, String nome, String tipo, UUID categoriaPaiId)
      throws Exception {
    String pai = categoriaPaiId == null ? "null" : "\"" + categoriaPaiId + "\"";
    String corpo =
        """
        {"usuarioId":"%s","nome":"%s","tipo":"%s","categoriaPaiId":%s}
        """
            .formatted(usuarioId, nome, tipo, pai);
    return idDe(postar("/api/categorias", corpo).andExpect(status().isCreated()));
  }

  protected ResultActions criarLancamento(
      UUID usuarioId,
      UUID contaId,
      UUID categoriaId,
      String tipo,
      String valor,
      LocalDate dataVencimento,
      LocalDate dataPagamento)
      throws Exception {
    return postar(
        "/api/lancamentos-financeiros",
        corpoLancamento(
            usuarioId, contaId, categoriaId, tipo, valor, dataVencimento, dataPagamento));
  }

  protected String corpoLancamento(
      UUID usuarioId,
      UUID contaId,
      UUID categoriaId,
      String tipo,
      String valor,
      LocalDate dataVencimento,
      LocalDate dataPagamento) {
    return """
        {"usuarioId":"%s","contaId":"%s","categoriaId":"%s","descricao":"Lançamento de teste",
         "tipo":"%s","valor":%s,"dataCompetencia":"%s","dataVencimento":%s,"dataPagamento":%s}
        """
        .formatted(
            usuarioId,
            contaId,
            categoriaId,
            tipo,
            valor,
            HOJE,
            textoOuNull(dataVencimento),
            textoOuNull(dataPagamento));
  }

  protected BigDecimal saldoAtual(UUID contaId, UUID usuarioId) throws Exception {
    String json =
        mockMvc
            .perform(get("/api/contas/{id}", contaId).param("usuarioId", usuarioId.toString()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return new BigDecimal(JsonPath.read(json, "$.saldoAtual").toString());
  }

  protected ResultActions postar(String url, String corpo) throws Exception {
    return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(corpo));
  }

  protected ResultActions atualizar(String url, String corpo) throws Exception {
    return mockMvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(corpo));
  }

  protected ResultActions alterar(String url, String corpo) throws Exception {
    return mockMvc.perform(patch(url).contentType(MediaType.APPLICATION_JSON).content(corpo));
  }

  protected ResultActions consultar(String url, UUID usuarioId) throws Exception {
    return mockMvc.perform(get(url).param("usuarioId", usuarioId.toString()));
  }

  protected ResultActions remover(String url, UUID usuarioId) throws Exception {
    return mockMvc.perform(delete(url).param("usuarioId", usuarioId.toString()));
  }

  protected ResultActions ativar(String url, UUID usuarioId) throws Exception {
    return mockMvc.perform(patch(url).param("usuarioId", usuarioId.toString()));
  }

  protected static UUID idDe(ResultActions resultado) throws Exception {
    String json = resultado.andReturn().getResponse().getContentAsString();
    return UUID.fromString(JsonPath.read(json, "$.id"));
  }

  private static String textoOuNull(LocalDate data) {
    return data == null ? "null" : "\"" + data + "\"";
  }
}
