package br.com.financeiro.api.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

@DisplayName("Autenticação")
class AutenticacaoIntegracaoTest extends IntegracaoTest {

  @Test
  @DisplayName("cadastro devolve os tokens e o usuário, com e-mail em minúsculas")
  void cadastro() throws Exception {
    String email = "Fulano." + UUID.randomUUID() + "@Teste.Local";

    Sessao sessao = cadastrar(email);

    assertThat(sessao.email()).isEqualTo(email.toLowerCase());
    assertThat(sessao.accessToken()).isNotBlank();
    assertThat(sessao.refreshToken()).isNotBlank().isNotEqualTo(sessao.accessToken());
  }

  @Test
  @DisplayName("e-mail repetido é rejeitado sem diferenciar maiúsculas")
  void emailRepetido() throws Exception {
    String email = UUID.randomUUID() + "@teste.local";
    cadastrar(email);

    publico("/api/auth/cadastro", corpoCadastro(email.toUpperCase(), SENHA_PADRAO))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Já existe um usuário cadastrado com esse e-mail."));
  }

  @Test
  @DisplayName("cadastro valida e-mail e tamanho da senha")
  void cadastroInvalido() throws Exception {
    publico("/api/auth/cadastro", corpoCadastro("nao-e-email", "curta"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.length()").value(2));
  }

  @Test
  @DisplayName("login com a senha certa devolve tokens que dão acesso à API")
  void loginValido() throws Exception {
    Sessao cadastrado = criarUsuario();

    String json =
        publico("/api/auth/login", corpoLogin(cadastrado.email().toUpperCase(), SENHA_PADRAO))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tipo").value("Bearer"))
            .andExpect(jsonPath("$.expiraEm").value(900))
            .andReturn()
            .getResponse()
            .getContentAsString();

    Sessao logado = sessaoDe(json);
    consultar("/api/auth/eu", logado)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(cadastrado.id().toString()))
        .andExpect(jsonPath("$.email").value(cadastrado.email()));
  }

  @Test
  @DisplayName("senha errada e e-mail inexistente dão a mesma resposta 401")
  void loginInvalido() throws Exception {
    Sessao cadastrado = criarUsuario();

    publico("/api/auth/login", corpoLogin(cadastrado.email(), "senha-errada-123"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));
    publico("/api/auth/login", corpoLogin("ninguem@teste.local", SENHA_PADRAO))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));
  }

  @Test
  @DisplayName("refresh token gera um novo par de tokens")
  void refresh() throws Exception {
    Sessao sessao = criarUsuario();

    String json =
        publico("/api/auth/refresh", corpoRefresh(sessao.refreshToken()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    consultar("/api/auth/eu", sessaoDe(json)).andExpect(status().isOk());
  }

  @Test
  @DisplayName("token de acesso não serve como refresh, e refresh não serve como acesso")
  void tiposDeTokenNaoSeMisturam() throws Exception {
    Sessao sessao = criarUsuario();

    publico("/api/auth/refresh", corpoRefresh(sessao.accessToken()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Refresh token inválido ou expirado."));

    mockMvc
        .perform(
            get("/api/contas").header(HttpHeaders.AUTHORIZATION, "Bearer " + sessao.refreshToken()))
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/api/contas",
        "/api/categorias",
        "/api/lancamentos-financeiros",
        "/api/dashboard-financeiro/resumo-mensal?ano=2026&mes=6",
        "/api/auth/eu"
      })
  @DisplayName("sem token, todo endpoint protegido devolve 401 no envelope padrão")
  void semToken(String url) throws Exception {
    mockMvc
        .perform(get(url))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value("Token de acesso ausente, inválido ou expirado."));
  }

  @Test
  @DisplayName("token adulterado devolve 401")
  void tokenAdulterado() throws Exception {
    Sessao sessao = criarUsuario();
    String adulterado = sessao.accessToken().substring(0, sessao.accessToken().length() - 2) + "xx";

    mockMvc
        .perform(get("/api/contas").header(HttpHeaders.AUTHORIZATION, "Bearer " + adulterado))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Swagger e health continuam públicos")
  void rotasPublicas() throws Exception {
    mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("o Swagger não expõe usuarioId como parâmetro")
  void swaggerSemUsuarioId() throws Exception {
    String json =
        mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();

    assertThat(json).doesNotContain("\"name\":\"usuarioId\"");
    assertThat(json).contains("bearer-jwt");
  }

  @Test
  @DisplayName("CORS libera o front do Angular")
  void cors() throws Exception {
    mockMvc
        .perform(
            options("/api/contas")
                .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));

    mockMvc
        .perform(
            options("/api/contas")
                .header(HttpHeaders.ORIGIN, "http://site-malicioso.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isForbidden());
  }

  private ResultActions publico(String url, String corpo) throws Exception {
    return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(corpo));
  }

  private static String corpoCadastro(String email, String senha) {
    return """
        {"nome":"Fulano","email":"%s","senha":"%s"}
        """
        .formatted(email, senha);
  }

  private static String corpoLogin(String email, String senha) {
    return """
        {"email":"%s","senha":"%s"}
        """
        .formatted(email, senha);
  }

  private static String corpoRefresh(String token) {
    return """
        {"refreshToken":"%s"}
        """
        .formatted(token);
  }
}
