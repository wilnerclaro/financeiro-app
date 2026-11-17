package br.com.financeiro.api.integracao;

import static br.com.financeiro.api.importacao.OfxDeTeste.documento;
import static br.com.financeiro.api.importacao.OfxDeTeste.transacao;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

class ImportacaoOfxIntegracaoTest extends IntegracaoTest {
  @Test
  void deveImportarUmaVezAtualizandoSaldoEDataBancaria() throws Exception {
    Sessao usuario = criarUsuario();
    UUID conta = criarConta(usuario, "Banco", "100");
    UUID categoria = criarCategoria(usuario, "Compras", "DESPESA", null);
    String arquivo = documento(transacao("001", "-15.0000"));
    String id = identificador(usuario, conta, arquivo, 0);
    String selecoes = "[" + selecao(id, categoria) + "]";
    confirmar(usuario, conta, arquivo, selecoes)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.importados").value(1));
    assertThat(saldoAtual(conta, usuario)).isEqualByComparingTo("85");
    revisar(usuario, conta, arquivo).andExpect(jsonPath("$.itens[0].jaImportado").value(true));
    confirmar(usuario, conta, arquivo, selecoes)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.importados").value(0))
        .andExpect(jsonPath("$.ignorados").value(1));
    assertThat(saldoAtual(conta, usuario)).isEqualByComparingTo("85");
    consultar("/api/lancamentos-financeiros", usuario)
        .andExpect(jsonPath("$.content[0].dataPagamento").value("2026-06-10"))
        .andExpect(jsonPath("$.content[0].valor").value(15));
  }

  @Test
  void deveReverterLoteInteiroQuandoSegundaCategoriaPertenceAOutroUsuario() throws Exception {
    Sessao usuario = criarUsuario();
    Sessao outro = criarUsuario();
    UUID conta = criarConta(usuario, "Banco", "100");
    UUID categoria = criarCategoria(usuario, "Compras", "DESPESA", null);
    UUID estrangeira = criarCategoria(outro, "Compras", "DESPESA", null);
    String arquivo = documento(transacao("001", "-15") + transacao("002", "-20"));
    String primeiro = identificador(usuario, conta, arquivo, 0);
    String segundo = identificador(usuario, conta, arquivo, 1);
    confirmar(
            usuario,
            conta,
            arquivo,
            "[" + selecao(primeiro, categoria) + "," + selecao(segundo, estrangeira) + "]")
        .andExpect(status().isNotFound());
    assertThat(saldoAtual(conta, usuario)).isEqualByComparingTo("100");
    revisar(usuario, conta, arquivo)
        .andExpect(jsonPath("$.itens[0].jaImportado").value(false))
        .andExpect(jsonPath("$.itens[1].jaImportado").value(false));
    consultar("/api/lancamentos-financeiros", usuario)
        .andExpect(jsonPath("$.content.length()").value(0));
    revisar(outro, conta, arquivo).andExpect(status().isNotFound());
  }

  @Test
  void deveValidarSelecoesEConta() throws Exception {
    Sessao usuario = criarUsuario();
    UUID conta = criarConta(usuario, "Banco", "100");
    UUID categoria = criarCategoria(usuario, "Compras", "DESPESA", null);
    String arquivo = documento(transacao("001", "-15"));
    String item = selecao(identificador(usuario, conta, arquivo, 0), categoria);
    for (String invalida :
        new String[] {
          "[]",
          "null",
          "{",
          "[null]",
          "[" + item + "," + item + "]",
          "[" + selecao("desconhecido", categoria) + "]"
        }) {
      confirmar(usuario, conta, arquivo, invalida).andExpect(status().isBadRequest());
    }
    UUID cartao = criarCartao(usuario, "Cartão", 10, 20);
    revisar(usuario, cartao, arquivo).andExpect(status().isBadRequest());
    mockMvc
        .perform(
            multipart("/api/importacoes/ofx/revisar")
                .param("contaId", conta.toString())
                .header(HttpHeaders.AUTHORIZATION, usuario.bearer()))
        .andExpect(status().isBadRequest());
    assertThat(saldoAtual(conta, usuario)).isEqualByComparingTo("100");
  }

  @Test
  void deveSerializarConfirmacoesConcorrentesNaMesmaConta() throws Exception {
    Sessao usuario = criarUsuario();
    UUID conta = criarConta(usuario, "Banco", "100");
    UUID categoria = criarCategoria(usuario, "Compras", "DESPESA", null);
    String arquivo = documento(transacao("001", "-15"));
    String selecoes = "[" + selecao(identificador(usuario, conta, arquivo, 0), categoria) + "]";
    try (var executor = Executors.newFixedThreadPool(2)) {
      var primeiro =
          executor.submit(
              () -> confirmar(usuario, conta, arquivo, selecoes).andExpect(status().isOk()));
      var segundo =
          executor.submit(
              () -> confirmar(usuario, conta, arquivo, selecoes).andExpect(status().isOk()));
      primeiro.get();
      segundo.get();
    }
    assertThat(saldoAtual(conta, usuario)).isEqualByComparingTo("85");
    consultar("/api/lancamentos-financeiros", usuario)
        .andExpect(jsonPath("$.content.length()").value(1));
  }

  private String identificador(Sessao usuario, UUID conta, String arquivo, int indice)
      throws Exception {
    String json =
        revisar(usuario, conta, arquivo)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(json, "$.itens[" + indice + "].identificador");
  }

  private String selecao(String identificador, UUID categoria) {
    return "{\"identificador\":\"" + identificador + "\",\"categoriaId\":\"" + categoria + "\"}";
  }

  private ResultActions revisar(Sessao usuario, UUID conta, String arquivo) throws Exception {
    return mockMvc.perform(
        multipart("/api/importacoes/ofx/revisar")
            .file(arquivo(arquivo))
            .param("contaId", conta.toString())
            .header(HttpHeaders.AUTHORIZATION, usuario.bearer()));
  }

  private ResultActions confirmar(Sessao usuario, UUID conta, String arquivo, String selecoes)
      throws Exception {
    return mockMvc.perform(
        multipart("/api/importacoes/ofx/confirmar")
            .file(arquivo(arquivo))
            .param("contaId", conta.toString())
            .param("selecoes", selecoes)
            .header(HttpHeaders.AUTHORIZATION, usuario.bearer()));
  }

  private MockMultipartFile arquivo(String texto) {
    return new MockMultipartFile(
        "arquivo", "extrato.ofx", "application/x-ofx", texto.getBytes(StandardCharsets.UTF_8));
  }
}
