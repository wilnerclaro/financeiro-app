package br.com.financeiro.api.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void configurar() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("ResourceNotFoundException vira 404 com o envelope ApiErrorResponse")
  void recursoNaoEncontrado() throws Exception {
    mockMvc
        .perform(get("/teste/nao-encontrado"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Conta nao encontrada."))
        .andExpect(jsonPath("$.path").value("/teste/nao-encontrado"))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.fieldErrors").isEmpty());
  }

  @Test
  @DisplayName("BusinessException vira 400 com a mensagem da regra")
  void excecaoDeNegocio() throws Exception {
    mockMvc
        .perform(get("/teste/negocio"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("A conta informada esta inativa."));
  }

  @Test
  @DisplayName("erro de validacao vira 400 com fieldErrors por campo")
  void erroDeValidacao() throws Exception {
    mockMvc
        .perform(
            post("/teste/validacao")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Existem campos invalidos na requisicao."))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("nome"))
        .andExpect(jsonPath("$.fieldErrors[0].message").value("O nome e obrigatorio."));
  }

  @Test
  @DisplayName("excecao inesperada vira 500 sem expor detalhe interno")
  void erroGenerico() throws Exception {
    mockMvc
        .perform(get("/teste/inesperado"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.message").value("Erro interno inesperado."));
  }

  record RequisicaoDeTeste(@NotBlank(message = "O nome e obrigatorio.") String nome) {}

  @RestController
  static class ControllerDeTeste {

    @GetMapping("/teste/nao-encontrado")
    void naoEncontrado() {
      throw new ResourceNotFoundException("Conta nao encontrada.");
    }

    @GetMapping("/teste/negocio")
    void negocio() {
      throw new BusinessException("A conta informada esta inativa.");
    }

    @PostMapping("/teste/validacao")
    void validacao(@Valid @RequestBody RequisicaoDeTeste requisicao) {}

    @GetMapping("/teste/inesperado")
    void inesperado() {
      throw new IllegalStateException("detalhe interno que nao deve vazar");
    }
  }
}
