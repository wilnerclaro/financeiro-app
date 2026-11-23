package br.com.financeiro.api.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.financeiro.api.importacao.PdfDeTeste;
import br.com.financeiro.api.importacao.dto.TransacaoLida;
import br.com.financeiro.api.importacao.service.LeitorDeExtratoCsv;
import br.com.financeiro.api.importacao.service.LeitorDeExtratoPdf;
import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

class ImportacaoExtratoLeitoresIntegracaoTest extends IntegracaoTest {
  private static final String BANCO = "Banco Teste";
  private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  @TestConfiguration
  static class LeitoresDeTeste {
    @Bean
    LeitorDeExtratoPdf leitorPdfDeTeste() {
      return new LeitorDeExtratoPdf() {
        @Override
        public String banco() {
          return BANCO;
        }

        @Override
        public boolean reconhece(List<String> linhas) {
          return linhas.getFirst().equals("BANCO TESTE");
        }

        @Override
        public List<TransacaoLida> ler(List<String> linhas) {
          return linhas.stream().skip(1).map(linha -> transacao(linha.split(";"))).toList();
        }
      };
    }

    @Bean
    LeitorDeExtratoCsv leitorCsvDeTeste() {
      return new LeitorDeExtratoCsv() {
        @Override
        public String banco() {
          return BANCO;
        }

        @Override
        public boolean reconhece(List<List<String>> linhas) {
          return linhas.getFirst().equals(List.of("Data", "Descricao", "Valor"));
        }

        @Override
        public List<TransacaoLida> ler(List<List<String>> linhas) {
          return linhas.stream()
              .skip(1)
              .map(linha -> transacao(linha.toArray(String[]::new)))
              .toList();
        }
      };
    }

    private static TransacaoLida transacao(String[] campos) {
      BigDecimal valor = new BigDecimal(campos[2].replace(",", "."));
      return new TransacaoLida(
          null,
          LocalDate.parse(campos[0], DATA),
          campos[1],
          valor.abs(),
          valor.signum() < 0 ? TipoLancamentoFinanceiro.DESPESA : TipoLancamentoFinanceiro.RECEITA,
          null);
    }
  }

  @Test
  void deveLerPdfECsvDoMesmoBancoSemDuplicarEntreFormatos() throws Exception {
    Sessao usuario = criarUsuario();
    UUID conta = criarConta(usuario, "Banco", "100");
    UUID categoria = criarCategoria(usuario, "Compras", "DESPESA", null);
    byte[] pdf =
        PdfDeTeste.pdf(
            "BANCO TESTE",
            "10/06/2026;Padaria;-5,00",
            "10/06/2026;Padaria;-5,00",
            "11/06/2026;Mercado;-20,00");
    String json =
        revisar(usuario, conta, "extrato.pdf", pdf)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.formato").value("PDF"))
            .andExpect(jsonPath("$.banco").value(BANCO))
            .andExpect(jsonPath("$.itens.length()").value(3))
            .andReturn()
            .getResponse()
            .getContentAsString();
    List<String> ids = JsonPath.read(json, "$.itens[*].identificador");
    assertThat(ids).doesNotHaveDuplicates();

    String selecoes =
        "[{\"identificador\":\"%s\",\"categoriaId\":\"%s\"},{\"identificador\":\"%s\",\"categoriaId\":\"%s\"}]"
            .formatted(ids.get(0), categoria, ids.get(1), categoria);
    mockMvc
        .perform(
            multipart("/api/importacoes/confirmar")
                .file(new MockMultipartFile("arquivo", "extrato.pdf", "application/pdf", pdf))
                .param("contaId", conta.toString())
                .param("selecoes", selecoes)
                .header(HttpHeaders.AUTHORIZATION, usuario.bearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.importados").value(2));
    assertThat(saldoAtual(conta, usuario)).isEqualByComparingTo("90");
    consultar("/api/lancamentos-financeiros", usuario)
        .andExpect(jsonPath("$.content[0].observacao").value("Importado de extrato PDF"));

    byte[] csv =
        "Data;Descricao;Valor\n10/06/2026;Padaria;-5,00\n10/06/2026;Padaria;-5,00\n11/06/2026;Mercado;-20,00"
            .getBytes(StandardCharsets.UTF_8);
    revisar(usuario, conta, "extrato.csv", csv)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.formato").value("CSV"))
        .andExpect(jsonPath("$.itens[0].jaImportado").value(true))
        .andExpect(jsonPath("$.itens[1].jaImportado").value(true))
        .andExpect(jsonPath("$.itens[2].jaImportado").value(false))
        .andExpect(jsonPath("$.itens[2].possivelDuplicado").value(false));
  }

  private ResultActions revisar(Sessao usuario, UUID conta, String nome, byte[] conteudo)
      throws Exception {
    return mockMvc.perform(
        multipart("/api/importacoes/revisar")
            .file(new MockMultipartFile("arquivo", nome, "application/octet-stream", conteudo))
            .param("contaId", conta.toString())
            .header(HttpHeaders.AUTHORIZATION, usuario.bearer()));
  }
}
