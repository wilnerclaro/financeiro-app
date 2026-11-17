package br.com.financeiro.api.importacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.importacao.service.TextoPdfService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class TextoPdfServiceTest {
  private final TextoPdfService texto = new TextoPdfService();

  @Test
  void devolveAsLinhasComTextoNaOrdemDaPagina() {
    assertThat(texto.linhas(PdfDeTeste.pdf("EXTRATO", "", "10/06/2026  Mercado  -15,00")))
        .containsExactly("EXTRATO", "10/06/2026  Mercado  -15,00");
  }

  @Test
  void recusaPdfSemTextoComSenhaGrandeOuCorrompido() {
    assertThatThrownBy(() -> texto.linhas(PdfDeTeste.pdf()))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("digitalizado");
    assertThatThrownBy(() -> texto.linhas(PdfDeTeste.comSenha("EXTRATO")))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("senha");
    assertThatThrownBy(() -> texto.linhas(PdfDeTeste.comPaginas(61)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("60 páginas");
    assertThatThrownBy(() -> texto.linhas("%PDF-1.7 quebrado".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Não foi possível ler o PDF");
  }
}
