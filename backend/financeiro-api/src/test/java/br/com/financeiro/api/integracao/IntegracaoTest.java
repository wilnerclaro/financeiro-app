package br.com.financeiro.api.integracao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financeiro.api.TestcontainersConfiguration;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = IntegracaoTest.SEGREDO_JWT_DE_TESTE)
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, IntegracaoTest.RelogioFixo.class})
abstract class IntegracaoTest {

  static final String SEGREDO_JWT_DE_TESTE =
      "app.jwt.segredo=segredo-apenas-para-testes-com-mais-de-32-bytes";

  static final LocalDate HOJE = LocalDate.of(2026, 6, 15);

  static final String SENHA_PADRAO = "senha-forte-123";

  @Autowired protected MockMvc mockMvc;

  record Sessao(UUID id, String email, String accessToken, String refreshToken) {

    String bearer() {
      return "Bearer " + accessToken;
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class RelogioFixo {

    @Bean
    @Primary
    Clock relogioFixo() {
      ZoneId fuso = ZoneId.of("America/Sao_Paulo");
      return Clock.fixed(HOJE.atStartOfDay(fuso).plusHours(12).toInstant(), fuso);
    }
  }

  protected Sessao criarUsuario() throws Exception {
    return cadastrar(UUID.randomUUID() + "@teste.local");
  }

  protected Sessao cadastrar(String email) throws Exception {
    String corpo =
        """
        {"nome":"Usuário de teste","email":"%s","senha":"%s"}
        """
            .formatted(email, SENHA_PADRAO);
    String json =
        mockMvc
            .perform(
                post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return sessaoDe(json);
  }

  protected static Sessao sessaoDe(String json) {
    return new Sessao(
        UUID.fromString(JsonPath.read(json, "$.usuario.id")),
        JsonPath.read(json, "$.usuario.email"),
        JsonPath.read(json, "$.accessToken"),
        JsonPath.read(json, "$.refreshToken"));
  }

  protected UUID criarConta(Sessao usuario, String nome, String saldoInicial) throws Exception {
    String corpo =
        """
        {"nome":"%s","tipo":"CONTA_CORRENTE","saldoInicial":%s}
        """
            .formatted(nome, saldoInicial);
    return idDe(postar(usuario, "/api/contas", corpo).andExpect(status().isCreated()));
  }

  protected UUID criarCategoria(Sessao usuario, String nome, String tipo, UUID categoriaPaiId)
      throws Exception {
    String pai = categoriaPaiId == null ? "null" : "\"" + categoriaPaiId + "\"";
    String corpo =
        """
        {"nome":"%s","tipo":"%s","categoriaPaiId":%s}
        """
            .formatted(nome, tipo, pai);
    return idDe(postar(usuario, "/api/categorias", corpo).andExpect(status().isCreated()));
  }

  protected ResultActions criarLancamento(
      Sessao usuario,
      UUID contaId,
      UUID categoriaId,
      String tipo,
      String valor,
      LocalDate dataVencimento,
      LocalDate dataPagamento)
      throws Exception {
    return postar(
        usuario,
        "/api/lancamentos-financeiros",
        corpoLancamento(contaId, categoriaId, tipo, valor, dataVencimento, dataPagamento));
  }

  protected String corpoLancamento(
      UUID contaId,
      UUID categoriaId,
      String tipo,
      String valor,
      LocalDate dataVencimento,
      LocalDate dataPagamento) {
    return corpoLancamento(contaId, categoriaId, tipo, valor, HOJE, dataVencimento, dataPagamento);
  }

  protected ResultActions criarLancamentoEm(
      Sessao usuario,
      UUID contaId,
      UUID categoriaId,
      String tipo,
      String valor,
      LocalDate dataCompetencia,
      LocalDate dataVencimento,
      LocalDate dataPagamento)
      throws Exception {
    return postar(
        usuario,
        "/api/lancamentos-financeiros",
        corpoLancamento(
            contaId, categoriaId, tipo, valor, dataCompetencia, dataVencimento, dataPagamento));
  }

  protected String corpoLancamento(
      UUID contaId,
      UUID categoriaId,
      String tipo,
      String valor,
      LocalDate dataCompetencia,
      LocalDate dataVencimento,
      LocalDate dataPagamento) {
    return """
        {"contaId":"%s","categoriaId":"%s","descricao":"Lançamento de teste",
         "tipo":"%s","valor":%s,"dataCompetencia":"%s","dataVencimento":%s,"dataPagamento":%s}
        """
        .formatted(
            contaId,
            categoriaId,
            tipo,
            valor,
            dataCompetencia,
            textoOuNull(dataVencimento),
            textoOuNull(dataPagamento));
  }

  protected UUID categoriaPadrao(Sessao usuario, String nome) throws Exception {
    String json =
        mockMvc
            .perform(autenticado(get("/api/categorias"), usuario).param("size", "200"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    java.util.List<String> ids = JsonPath.read(json, "$.content[?(@.nome == '" + nome + "')].id");
    if (ids.size() != 1) {
      throw new IllegalStateException("Categoria padrão ambígua ou ausente: " + nome);
    }
    return UUID.fromString(ids.getFirst());
  }

  protected UUID criarCartao(Sessao usuario, String nome, int diaFechamento, int diaVencimento)
      throws Exception {
    String corpo =
        """
        {"nome":"%s","tipo":"CARTAO_CREDITO","saldoInicial":0,
         "diaFechamento":%d,"diaVencimento":%d}
        """
            .formatted(nome, diaFechamento, diaVencimento);
    return idDe(postar(usuario, "/api/contas", corpo).andExpect(status().isCreated()));
  }

  protected BigDecimal saldoAtual(UUID contaId, Sessao usuario) throws Exception {
    String json =
        consultar("/api/contas/" + contaId, usuario)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return new BigDecimal(JsonPath.read(json, "$.saldoAtual").toString());
  }

  protected ResultActions postar(Sessao usuario, String url, String corpo) throws Exception {
    return mockMvc.perform(comCorpo(post(url), usuario, corpo));
  }

  protected ResultActions atualizar(Sessao usuario, String url, String corpo) throws Exception {
    return mockMvc.perform(comCorpo(put(url), usuario, corpo));
  }

  protected ResultActions alterar(Sessao usuario, String url, String corpo) throws Exception {
    return mockMvc.perform(comCorpo(patch(url), usuario, corpo));
  }

  protected ResultActions consultar(String url, Sessao usuario) throws Exception {
    return mockMvc.perform(autenticado(get(url), usuario));
  }

  protected ResultActions remover(String url, Sessao usuario) throws Exception {
    return mockMvc.perform(autenticado(delete(url), usuario));
  }

  protected ResultActions ativar(String url, Sessao usuario) throws Exception {
    return mockMvc.perform(autenticado(patch(url), usuario));
  }

  protected static MockHttpServletRequestBuilder autenticado(
      MockHttpServletRequestBuilder requisicao, Sessao usuario) {
    return requisicao.header(HttpHeaders.AUTHORIZATION, usuario.bearer());
  }

  protected static UUID idDe(ResultActions resultado) throws Exception {
    String json = resultado.andReturn().getResponse().getContentAsString();
    return UUID.fromString(JsonPath.read(json, "$.id"));
  }

  private static MockHttpServletRequestBuilder comCorpo(
      MockHttpServletRequestBuilder requisicao, Sessao usuario, String corpo) {
    return autenticado(requisicao, usuario).contentType(MediaType.APPLICATION_JSON).content(corpo);
  }

  private static String textoOuNull(LocalDate data) {
    return data == null ? "null" : "\"" + data + "\"";
  }
}
