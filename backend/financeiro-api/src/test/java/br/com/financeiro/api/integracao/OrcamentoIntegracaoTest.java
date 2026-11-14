package br.com.financeiro.api.integracao;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Orçamento: Previsto x Realizado")
class OrcamentoIntegracaoTest extends IntegracaoTest {

  private static final String URL = "/api/orcamentos";

  private Sessao usuario;
  private UUID banco;
  private UUID supermercado;
  private UUID restaurante;
  private UUID salarios;
  private UUID dividendos;

  @BeforeEach
  void prepararDados() throws Exception {
    usuario = criarUsuario();
    banco = criarConta(usuario, "Banco", "0.00");
    supermercado = categoriaPadrao(usuario, "Supermercado");
    restaurante = categoriaPadrao(usuario, "Restaurante");
    salarios = categoriaPadrao(usuario, "Salários");
    dividendos = categoriaPadrao(usuario, "Dividendos");
  }

  @Test
  @DisplayName("grupo soma os subitens, com desvio, percentual, fontes e saldo")
  void previstoRealizado() throws Exception {
    definir(
            2026,
            6,
            item(supermercado, "1000.00"),
            item(restaurante, "500.00"),
            item(salarios, "5000.00"),
            item(dividendos, "200.00"))
        .andExpect(status().isOk());

    pagar(supermercado, "DESPESA", "950.00", HOJE);
    pagar(restaurante, "DESPESA", "600.00", HOJE);
    pagar(salarios, "RECEITA", "5000.00", HOJE);
    pagar(dividendos, "RECEITA", "100.00", HOJE);

    consultar(URL + "/2026/6", usuario)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.despesas.total.previsto").value(1500.00))
        .andExpect(jsonPath("$.despesas.total.realizado").value(1550.00))
        .andExpect(grupo("despesas", "Alimentação", "valores.previsto").value(1500.00))
        .andExpect(grupo("despesas", "Alimentação", "valores.realizado").value(1550.00))
        .andExpect(grupo("despesas", "Alimentação", "valores.desvio").value(50.00))
        .andExpect(grupo("despesas", "Alimentação", "valores.percentualRealizado").value(1.0333))
        .andExpect(grupo("despesas", "Moradia", "valores.previsto").value(0.00))
        .andExpect(grupo("despesas", "Moradia", "valores.percentualRealizado").value(0.0))
        .andExpect(jsonPath("$.receitasFonteAtiva.realizado").value(5000.00))
        .andExpect(jsonPath("$.receitasFontePassiva.previsto").value(200.00))
        .andExpect(jsonPath("$.receitasFontePassiva.realizado").value(100.00))
        .andExpect(jsonPath("$.receitasFontePassiva.percentualRealizado").value(0.5))
        .andExpect(jsonPath("$.saldo.previsto").value(3700.00))
        .andExpect(jsonPath("$.saldo.realizado").value(3550.00));
  }

  @Test
  @DisplayName("definir de novo substitui o valor do mês")
  void substitui() throws Exception {
    definir(2026, 6, item(supermercado, "1000.00")).andExpect(status().isOk());
    definir(2026, 6, item(supermercado, "800.00"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.despesas.total.previsto").value(800.00));
  }

  @Test
  @DisplayName("repete o previsto de uma categoria em vários meses, mantendo ou substituindo")
  void repetir() throws Exception {
    definir(2027, 1, item(supermercado, "300.00")).andExpect(status().isOk());

    postar(
            usuario,
            URL + "/repetir",
            repeticao(supermercado, "900.00", false, "2026-11", "2026-12", "2027-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meses.length()").value(3))
        .andExpect(jsonPath("$.meses[0].competencia").value("2026-11"))
        .andExpect(jsonPath("$.meses[0].gravadas").value(1))
        .andExpect(jsonPath("$.meses[2].competencia").value("2027-01"))
        .andExpect(jsonPath("$.meses[2].mantidas").value(1));

    consultar(URL + "/2026/12", usuario)
        .andExpect(jsonPath("$.despesas.total.previsto").value(900.00));
    consultar(URL + "/2027/1", usuario)
        .andExpect(jsonPath("$.despesas.total.previsto").value(300.00));

    postar(usuario, URL + "/repetir", repeticao(supermercado, "950.00", true, "2027-01"))
        .andExpect(jsonPath("$.meses[0].gravadas").value(1));
    consultar(URL + "/2027/1", usuario)
        .andExpect(jsonPath("$.despesas.total.previsto").value(950.00));

    definir(2026, 12, item(supermercado, "100.00")).andExpect(status().isOk());
    consultar(URL + "/2026/11", usuario)
        .andExpect(jsonPath("$.despesas.total.previsto").value(900.00));

    postar(usuario, URL + "/repetir", repeticao(supermercado, "1.00", false))
        .andExpect(status().isBadRequest());
    postar(
            usuario,
            URL + "/repetir",
            repeticao(categoriaPadrao(usuario, "Moradia"), "1.00", false, "2026-11"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("recusa previsto em grupo com subcategorias, negativo e categoria repetida")
  void validacoes() throws Exception {
    UUID moradia = categoriaPadrao(usuario, "Moradia");
    definir(2026, 6, item(moradia, "100.00")).andExpect(status().isBadRequest());
    definir(2026, 6, item(supermercado, "-1.00"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    definir(2026, 6, item(supermercado, "1.00"), item(supermercado, "2.00"))
        .andExpect(status().isBadRequest());
    definir(2026, 13, item(supermercado, "1.00")).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("não aceita categoria de outro usuário")
  void isolamento() throws Exception {
    Sessao outro = criarUsuario();
    UUID categoriaDoOutro = categoriaPadrao(outro, "Supermercado");
    definir(2026, 6, item(categoriaDoOutro, "100.00")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("copia o mês inteiro para outros meses, mantendo o que já existe")
  void copiarMes() throws Exception {
    definir(2026, 5, item(supermercado, "1000.00"), item(restaurante, "300.00"));
    definir(2026, 6, item(restaurante, "450.00"));

    postar(usuario, URL + "/2026/5/copiar", copia(false, "2026-06", "2026-07"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meses[0].gravadas").value(1))
        .andExpect(jsonPath("$.meses[0].mantidas").value(1))
        .andExpect(jsonPath("$.meses[1].gravadas").value(2));
    consultar(URL + "/2026/6", usuario)
        .andExpect(jsonPath("$.despesas.total.previsto").value(1450.00));
    consultar(URL + "/2026/7", usuario)
        .andExpect(jsonPath("$.despesas.total.previsto").value(1300.00));

    postar(usuario, URL + "/2026/5/copiar", copia(true, "2026-06"))
        .andExpect(jsonPath("$.meses[0].gravadas").value(2));
    consultar(URL + "/2026/6", usuario)
        .andExpect(jsonPath("$.despesas.total.previsto").value(1300.00));

    postar(usuario, URL + "/2026/1/copiar", copia(false, "2026-02"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("O mês de origem não tem previsto para copiar."));
    postar(usuario, URL + "/2026/5/copiar", copia(false, "2026-05"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("resumo do ano tem total e média só dos meses com valor")
  void resumoAnual() throws Exception {
    definir(2026, 5, item(supermercado, "1000.00"));
    definir(2026, 6, item(supermercado, "1200.00"));
    pagar(supermercado, "DESPESA", "900.00", LocalDate.of(2026, 5, 10));

    String linha = "$.linhas[?(@.nome == 'Supermercado')].";
    consultar(URL + "/2026", usuario)
        .andExpect(status().isOk())
        .andExpect(jsonPath(linha + "totalPrevisto").value(2200.00))
        .andExpect(jsonPath(linha + "mediaPrevisto").value(1100.00))
        .andExpect(jsonPath(linha + "totalRealizado").value(900.00))
        .andExpect(jsonPath(linha + "mediaRealizado").value(900.00))
        .andExpect(jsonPath("$.linhas[?(@.nome == 'Padaria')].mediaPrevisto").value(0.0));
  }

  private org.springframework.test.web.servlet.ResultActions definir(
      int ano, int mes, String... itens) throws Exception {
    return atualizar(
        usuario, URL + "/" + ano + "/" + mes, "{\"itens\":[" + String.join(",", itens) + "]}");
  }

  private static String repeticao(
      UUID categoriaId, String valor, boolean substituir, String... meses) {
    return """
        {"categoriaId":"%s","valorPrevisto":%s,"substituirExistentes":%s,"meses":%s}
        """
        .formatted(categoriaId, valor, substituir, listaDeMeses(meses));
  }

  private static String copia(boolean substituir, String... meses) {
    return """
        {"substituirExistentes":%s,"meses":%s}
        """
        .formatted(substituir, listaDeMeses(meses));
  }

  private static String listaDeMeses(String... meses) {
    return Arrays.stream(meses)
        .map(mes -> "\"" + mes + "\"")
        .collect(Collectors.joining(",", "[", "]"));
  }

  private static String item(UUID categoriaId, String valor) {
    return "{\"categoriaId\":\"" + categoriaId + "\",\"valorPrevisto\":" + valor + "}";
  }

  private void pagar(UUID categoria, String tipo, String valor, LocalDate data) throws Exception {
    criarLancamentoEm(usuario, banco, categoria, tipo, valor, data, null, data)
        .andExpect(status().isCreated());
  }

  private static org.springframework.test.web.servlet.result.JsonPathResultMatchers grupo(
      String bloco, String nome, String campo) {
    return jsonPath("$." + bloco + ".grupos[?(@.nome == '" + nome + "')]." + campo);
  }
}
