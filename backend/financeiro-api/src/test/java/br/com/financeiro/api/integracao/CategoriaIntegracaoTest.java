package br.com.financeiro.api.integracao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Categorias")
class CategoriaIntegracaoTest extends IntegracaoTest {

  private static final String URL = "/api/categorias";

  private UUID usuarioId;

  @BeforeEach
  void prepararDados() {
    usuarioId = criarUsuario();
  }

  @Test
  @DisplayName("o nome é guardado como digitado (sem forçar maiúsculas)")
  void nomeComoDigitado() throws Exception {
    UUID id = criarCategoria(usuarioId, "  Alimentação   fora ", "DESPESA", null);

    consultar(URL + "/" + id, usuarioId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Alimentação fora"))
        .andExpect(jsonPath("$.ativa").value(true))
        .andExpect(jsonPath("$.criadoEm").isNotEmpty());
  }

  @Test
  @DisplayName("nome duplicado é rejeitado sem diferenciar maiúsculas")
  void nomeDuplicado() throws Exception {
    criarCategoria(usuarioId, "Transporte", "DESPESA", null);

    postar(URL, corpo("TRANSPORTE", "DESPESA", null))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Já existe uma categoria com esse nome, tipo e categoria pai."));
  }

  @Test
  @DisplayName("não permite um terceiro nível")
  void terceiroNivel() throws Exception {
    UUID pai = criarCategoria(usuarioId, "Casa", "DESPESA", null);
    UUID filha = criarCategoria(usuarioId, "Contas", "DESPESA", pai);

    postar(URL, corpo("Luz", "DESPESA", filha)).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("categoria que já tem filhas não vira subcategoria pelo PUT")
  void categoriaComFilhasNaoViraSubcategoria() throws Exception {
    UUID casa = criarCategoria(usuarioId, "Casa", "DESPESA", null);
    criarCategoria(usuarioId, "Aluguel", "DESPESA", casa);
    UUID moradia = criarCategoria(usuarioId, "Moradia", "DESPESA", null);

    atualizar(URL + "/" + casa, corpo("Casa", "DESPESA", moradia))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Uma categoria que possui subcategorias não pode virar subcategoria. "
                        + "São permitidos apenas dois níveis."));
  }

  @Test
  @DisplayName("não troca o tipo de categoria que tem filhas, mesmo inativas")
  void trocaDeTipoComFilhas() throws Exception {
    UUID casa = criarCategoria(usuarioId, "Casa", "DESPESA", null);
    UUID aluguel = criarCategoria(usuarioId, "Aluguel", "DESPESA", casa);
    remover(URL + "/" + aluguel, usuarioId).andExpect(status().isNoContent());

    atualizar(URL + "/" + casa, corpo("Casa", "RECEITA", null)).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PUT não mexe no status: categoria inativa continua inativa")
  void putNaoAlteraStatus() throws Exception {
    UUID id = criarCategoria(usuarioId, "Viagem", "DESPESA", null);
    remover(URL + "/" + id, usuarioId).andExpect(status().isNoContent());

    atualizar(URL + "/" + id, corpo("Viagens", "DESPESA", null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Viagens"))
        .andExpect(jsonPath("$.ativa").value(false));
  }

  @Test
  @DisplayName("não inativa pai com filha ativa nem reativa filha com pai inativo")
  void regrasDeInativacao() throws Exception {
    UUID pai = criarCategoria(usuarioId, "Saúde", "DESPESA", null);
    UUID filha = criarCategoria(usuarioId, "Farmácia", "DESPESA", pai);

    remover(URL + "/" + pai, usuarioId).andExpect(status().isBadRequest());

    remover(URL + "/" + filha, usuarioId).andExpect(status().isNoContent());
    remover(URL + "/" + pai, usuarioId).andExpect(status().isNoContent());

    ativar(URL + "/" + filha + "/ativar", usuarioId)
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Não é possível ativar uma subcategoria cuja categoria pai está inativa."));

    ativar(URL + "/" + pai + "/ativar", usuarioId)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ativa").value(true));
    ativar(URL + "/" + filha + "/ativar", usuarioId).andExpect(status().isOk());
  }

  @Test
  @DisplayName("filha precisa ter o mesmo tipo do pai, e o pai não pode estar inativo")
  void regrasDoPai() throws Exception {
    UUID pai = criarCategoria(usuarioId, "Renda", "RECEITA", null);

    postar(URL, corpo("Mercado", "DESPESA", pai)).andExpect(status().isBadRequest());

    remover(URL + "/" + pai, usuarioId).andExpect(status().isNoContent());
    postar(URL, corpo("Bônus", "RECEITA", pai)).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("a categoria não pode ser pai dela mesma")
  void paiDelaMesma() throws Exception {
    UUID id = criarCategoria(usuarioId, "Educação", "DESPESA", null);

    atualizar(URL + "/" + id, corpo("Educação", "DESPESA", id)).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("listagem filtra por tipo e por status")
  void listarComFiltros() throws Exception {
    criarCategoria(usuarioId, "Salário", "RECEITA", null);
    UUID inativa = criarCategoria(usuarioId, "Antiga", "DESPESA", null);
    criarCategoria(usuarioId, "Mercado", "DESPESA", null);
    remover(URL + "/" + inativa, usuarioId).andExpect(status().isNoContent());

    mockMvc
        .perform(
            get(URL)
                .param("usuarioId", usuarioId.toString())
                .param("tipo", "DESPESA")
                .param("ativa", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].nome").value("Mercado"));
  }

  @Test
  @DisplayName("usuário inexistente devolve 404")
  void usuarioInexistente() throws Exception {
    consultar(URL, UUID.randomUUID()).andExpect(status().isNotFound());
  }

  private String corpo(String nome, String tipo, UUID categoriaPaiId) {
    String pai = categoriaPaiId == null ? "null" : "\"" + categoriaPaiId + "\"";
    return """
        {"usuarioId":"%s","nome":"%s","tipo":"%s","categoriaPaiId":%s}
        """
        .formatted(usuarioId, nome, tipo, pai);
  }
}
