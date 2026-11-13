package br.com.financeiro.api.integracao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

@DisplayName("Categorias")
class CategoriaIntegracaoTest extends IntegracaoTest {

  private static final String URL = "/api/categorias";

  private Sessao usuario;

  @BeforeEach
  void prepararDados() throws Exception {
    usuario = criarUsuario();
  }

  @Test
  @DisplayName("o nome é guardado como digitado (sem forçar maiúsculas)")
  void nomeComoDigitado() throws Exception {
    UUID id = criarCategoria(usuario, "  Alimentação   fora ", "DESPESA", null);

    consultar(URL + "/" + id, usuario)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Alimentação fora"))
        .andExpect(jsonPath("$.ativa").value(true))
        .andExpect(jsonPath("$.criadoEm").isNotEmpty());
  }

  @Test
  @DisplayName("nome duplicado é rejeitado sem diferenciar maiúsculas")
  void nomeDuplicado() throws Exception {
    criarCategoria(usuario, "Mobilidade", "DESPESA", null);

    postar(usuario, URL, corpo("TRANSPORTE", "DESPESA", null))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Já existe uma categoria com esse nome, tipo e categoria pai."));
  }

  @Test
  @DisplayName("não permite um terceiro nível")
  void terceiroNivel() throws Exception {
    UUID pai = criarCategoria(usuario, "Casa", "DESPESA", null);
    UUID filha = criarCategoria(usuario, "Contas", "DESPESA", pai);

    postar(usuario, URL, corpo("Luz", "DESPESA", filha)).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("categoria que já tem filhas não vira subcategoria pelo PUT")
  void categoriaComFilhasNaoViraSubcategoria() throws Exception {
    UUID casa = criarCategoria(usuario, "Casa", "DESPESA", null);
    criarCategoria(usuario, "Aluguel", "DESPESA", casa);
    UUID moradia = criarCategoria(usuario, "Residência", "DESPESA", null);

    atualizar(usuario, URL + "/" + casa, corpo("Casa", "DESPESA", moradia))
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
    UUID casa = criarCategoria(usuario, "Casa", "DESPESA", null);
    UUID aluguel = criarCategoria(usuario, "Aluguel", "DESPESA", casa);
    remover(URL + "/" + aluguel, usuario).andExpect(status().isNoContent());

    atualizar(usuario, URL + "/" + casa, corpo("Casa", "RECEITA", null))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PUT não mexe no status: categoria inativa continua inativa")
  void putNaoAlteraStatus() throws Exception {
    UUID id = criarCategoria(usuario, "Viagem", "DESPESA", null);
    remover(URL + "/" + id, usuario).andExpect(status().isNoContent());

    atualizar(usuario, URL + "/" + id, corpo("Viagens", "DESPESA", null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Viagens"))
        .andExpect(jsonPath("$.ativa").value(false));
  }

  @Test
  @DisplayName("não inativa pai com filha ativa nem reativa filha com pai inativo")
  void regrasDeInativacao() throws Exception {
    UUID pai = criarCategoria(usuario, "Saúde", "DESPESA", null);
    UUID filha = criarCategoria(usuario, "Farmácia", "DESPESA", pai);

    remover(URL + "/" + pai, usuario).andExpect(status().isBadRequest());

    remover(URL + "/" + filha, usuario).andExpect(status().isNoContent());
    remover(URL + "/" + pai, usuario).andExpect(status().isNoContent());

    ativar(URL + "/" + filha + "/ativar", usuario)
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Não é possível ativar uma subcategoria cuja categoria pai está inativa."));

    ativar(URL + "/" + pai + "/ativar", usuario)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ativa").value(true));
    ativar(URL + "/" + filha + "/ativar", usuario).andExpect(status().isOk());
  }

  @Test
  @DisplayName("filha precisa ter o mesmo tipo do pai, e o pai não pode estar inativo")
  void regrasDoPai() throws Exception {
    UUID pai = criarCategoria(usuario, "Renda", "RECEITA", null);

    postar(usuario, URL, corpo("Mercado", "DESPESA", pai)).andExpect(status().isBadRequest());

    remover(URL + "/" + pai, usuario).andExpect(status().isNoContent());
    postar(usuario, URL, corpo("Bônus", "RECEITA", pai)).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("a categoria não pode ser pai dela mesma")
  void paiDelaMesma() throws Exception {
    UUID id = criarCategoria(usuario, "Cursos", "DESPESA", null);

    atualizar(usuario, URL + "/" + id, corpo("Cursos", "DESPESA", id))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("listagem filtra por tipo e por status")
  void listarComFiltros() throws Exception {
    criarCategoria(usuario, "Salário", "RECEITA", null);
    UUID inativa = criarCategoria(usuario, "Antiga", "DESPESA", null);
    criarCategoria(usuario, "Mercado", "DESPESA", null);
    remover(URL + "/" + inativa, usuario).andExpect(status().isNoContent());

    mockMvc
        .perform(
            get(URL)
                .header(HttpHeaders.AUTHORIZATION, usuario.bearer())
                .param("tipo", "DESPESA")
                .param("ativa", "false"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].nome").value("Antiga"));
  }

  @Test
  @DisplayName("cadastro já cria as categorias da planilha, com receita ativa e passiva")
  void categoriasPadrao() throws Exception {
    Sessao novo = criarUsuario();
    mockMvc
        .perform(get(URL).header(HttpHeaders.AUTHORIZATION, novo.bearer()).param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements").value(100));

    consultar(URL + "/" + categoriaPadrao(novo, "Dividendos"), novo)
        .andExpect(jsonPath("$.tipo").value("RECEITA"))
        .andExpect(jsonPath("$.fonteReceita").value("PASSIVA"));
    consultar(URL + "/" + categoriaPadrao(novo, "Salários"), novo)
        .andExpect(jsonPath("$.fonteReceita").value("ATIVA"));
    consultar(URL + "/" + categoriaPadrao(novo, "Supermercado"), novo)
        .andExpect(jsonPath("$.fonteReceita").value(org.hamcrest.Matchers.nullValue()));
  }

  @Test
  @DisplayName("receita sem fonte vira ATIVA, subcategoria herda a fonte e despesa não tem fonte")
  void fonteDaReceita() throws Exception {
    UUID pai =
        idDe(
            postar(
                    usuario,
                    URL,
                    """
                    {"nome":"Investimentos","tipo":"RECEITA","fonteReceita":"PASSIVA"}
                    """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fonteReceita").value("PASSIVA")));

    postar(
            usuario,
            URL,
            """
            {"nome":"Cupons","tipo":"RECEITA","fonteReceita":"ATIVA","categoriaPaiId":"%s"}
            """
                .formatted(pai))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.fonteReceita").value("PASSIVA"));

    postar(usuario, URL, corpo("Bicos", "RECEITA", null))
        .andExpect(jsonPath("$.fonteReceita").value("ATIVA"));

    postar(
            usuario,
            URL,
            """
            {"nome":"Assinaturas","tipo":"DESPESA","fonteReceita":"PASSIVA"}
            """)
        .andExpect(jsonPath("$.fonteReceita").value(org.hamcrest.Matchers.nullValue()));

    atualizar(
            usuario,
            URL + "/" + pai,
            """
            {"nome":"Investimentos","tipo":"RECEITA","fonteReceita":"ATIVA"}
            """)
        .andExpect(status().isOk());
    consultar(URL + "/" + categoriaPadraoDe(usuario, "Cupons"), usuario)
        .andExpect(jsonPath("$.fonteReceita").value("ATIVA"));
  }

  private UUID categoriaPadraoDe(Sessao sessao, String nome) throws Exception {
    return categoriaPadrao(sessao, nome);
  }

  private String corpo(String nome, String tipo, UUID categoriaPaiId) {
    String pai = categoriaPaiId == null ? "null" : "\"" + categoriaPaiId + "\"";
    return """
        {"nome":"%s","tipo":"%s","categoriaPaiId":%s}
        """
        .formatted(nome, tipo, pai);
  }
}
