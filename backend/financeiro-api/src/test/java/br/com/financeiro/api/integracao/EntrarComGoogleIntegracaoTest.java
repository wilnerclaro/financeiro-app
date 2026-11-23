package br.com.financeiro.api.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financeiro.api.security.IdentidadeGoogle;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@DisplayName("Entrar com Google")
class EntrarComGoogleIntegracaoTest extends IntegracaoTest {

  @Test
  @DisplayName("a configuração pública informa o Client ID do Google")
  void configuracao() throws Exception {
    mockMvc
        .perform(get("/api/auth/configuracao"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.googleClientId").value("client-id-de-teste"));
  }

  @Test
  @DisplayName("primeiro acesso cria o usuário com o nome do Google e as categorias padrão")
  void primeiroAcesso() throws Exception {
    String email = UUID.randomUUID() + "@gmail.com";
    String token = tokenGoogle(UUID.randomUUID().toString(), email.toUpperCase(), "Ana Souza");

    Sessao sessao = sessaoDe(entrar(token).andExpect(status().isOk()).andReturn());

    assertThat(sessao.email()).isEqualTo(email);
    consultar("/api/auth/eu", sessao).andExpect(jsonPath("$.nome").value("Ana Souza"));
    consultar("/api/categorias?size=1", sessao)
        .andExpect(jsonPath("$.page.totalElements").value(greaterThan(0)));
  }

  @Test
  @DisplayName("a mesma conta Google entra sempre no mesmo usuário")
  void mesmoSujeito() throws Exception {
    String token =
        tokenGoogle(UUID.randomUUID().toString(), UUID.randomUUID() + "@gmail.com", "Bia");

    Sessao primeira = sessaoDe(entrar(token).andReturn());
    Sessao segunda = sessaoDe(entrar(token).andExpect(status().isOk()).andReturn());

    assertThat(segunda.id()).isEqualTo(primeira.id());
  }

  @Test
  @DisplayName("e-mail que já tem conta com senha é ligado ao Google, e a senha continua valendo")
  void ligaContaExistente() throws Exception {
    Sessao cadastrado = criarUsuario();
    String token =
        tokenGoogle(UUID.randomUUID().toString(), cadastrado.email().toUpperCase(), "Outro nome");

    Sessao peloGoogle = sessaoDe(entrar(token).andExpect(status().isOk()).andReturn());

    assertThat(peloGoogle.id()).isEqualTo(cadastrado.id());
    consultar("/api/auth/eu", peloGoogle).andExpect(jsonPath("$.nome").value("Usuário de teste"));
    publico("/api/auth/login", corpoLogin(cadastrado.email(), SENHA_PADRAO))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("quem só usa o Google não entra por senha e não recadastra o e-mail")
  void usuarioSoDoGoogle() throws Exception {
    String email = UUID.randomUUID() + "@gmail.com";
    entrar(tokenGoogle(UUID.randomUUID().toString(), email, "Caio")).andExpect(status().isOk());

    publico("/api/auth/login", corpoLogin(email, SENHA_PADRAO))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("E-mail ou senha inválidos."));
    publico(
            "/api/auth/cadastro",
            """
            {"nome":"Caio","email":"%s","senha":"%s"}
            """
                .formatted(email, SENHA_PADRAO))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("sem nome no Google, usa o começo do e-mail; nome longo é cortado em 120")
  void nomeDoUsuario() throws Exception {
    String prefixo = "sem.nome." + UUID.randomUUID().toString().substring(0, 8);
    Sessao semNome =
        sessaoDe(
            entrar(tokenGoogle(UUID.randomUUID().toString(), prefixo + "@gmail.com", " "))
                .andReturn());
    consultar("/api/auth/eu", semNome).andExpect(jsonPath("$.nome").value(prefixo));

    Sessao nomeLongo =
        sessaoDe(
            entrar(
                    tokenGoogle(
                        UUID.randomUUID().toString(),
                        UUID.randomUUID() + "@gmail.com",
                        "N".repeat(150)))
                .andReturn());
    consultar("/api/auth/eu", nomeLongo).andExpect(jsonPath("$.nome").value("N".repeat(120)));
  }

  @Test
  @DisplayName("token recusado devolve 401 e token vazio devolve 400 no campo")
  void tokenInvalido() throws Exception {
    entrar("token-que-o-google-nao-emitiu")
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Token do Google recusado."));
    entrar(" ")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("idToken"));
  }

  @Test
  @DisplayName("usuários do Google diferentes não enxergam os dados um do outro")
  void isolamento() throws Exception {
    Sessao usuarioA =
        sessaoDe(
            entrar(tokenGoogle(UUID.randomUUID().toString(), UUID.randomUUID() + "@gmail.com", "A"))
                .andReturn());
    Sessao usuarioB =
        sessaoDe(
            entrar(tokenGoogle(UUID.randomUUID().toString(), UUID.randomUUID() + "@gmail.com", "B"))
                .andReturn());
    UUID contaDeA = criarConta(usuarioA, "Banco de A", "100.00");

    assertThat(usuarioB.id()).isNotEqualTo(usuarioA.id());
    consultar("/api/contas/" + contaDeA, usuarioB).andExpect(status().isNotFound());
    consultar("/api/contas", usuarioB).andExpect(jsonPath("$.page.totalElements").value(0));
  }

  private static String tokenGoogle(String sujeito, String email, String nome) {
    String token = "google-" + UUID.randomUUID();
    TOKENS_GOOGLE.put(token, new IdentidadeGoogle(sujeito, email, nome));
    return token;
  }

  private ResultActions entrar(String idToken) throws Exception {
    return publico("/api/auth/google", "{\"idToken\":\"%s\"}".formatted(idToken));
  }

  private ResultActions publico(String url, String corpo) throws Exception {
    return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(corpo));
  }

  private static Sessao sessaoDe(MvcResult resultado) throws Exception {
    String json = resultado.getResponse().getContentAsString();
    assertThat(JsonPath.<String>read(json, "$.tipo")).isEqualTo("Bearer");
    return IntegracaoTest.sessaoDe(json);
  }

  private static String corpoLogin(String email, String senha) {
    return """
        {"email":"%s","senha":"%s"}
        """
        .formatted(email, senha);
  }
}
