package br.com.financeiro.api.importacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.importacao.service.TabelaCsvService;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class TabelaCsvServiceTest {
  private final TabelaCsvService tabela = new TabelaCsvService();

  private List<List<String>> ler(String texto) {
    return tabela.linhas(texto.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void detectaPontoEVirgulaAspasEQuebraDeLinhaDoWindows() {
    assertThat(ler("Data;Histórico;Valor\r\n10/06/2026;\"Pix; João \"\"Zé\"\"\";-1.234,56\r\n\r\n"))
        .containsExactly(
            List.of("Data", "Histórico", "Valor"),
            List.of("10/06/2026", "Pix; João \"Zé\"", "-1.234,56"));
  }

  @Test
  void detectaVirgulaETabulacao() {
    assertThat(ler("Data,Valor\n10/06/2026,\"-15,00\""))
        .element(1)
        .isEqualTo(List.of("10/06/2026", "-15,00"));
    assertThat(ler("Data\tValor\n10/06/2026\t-15"))
        .element(1)
        .isEqualTo(List.of("10/06/2026", "-15"));
    assertThat(ler("Só uma coluna\nvalor"))
        .containsExactly(List.of("Só uma coluna"), List.of("valor"));
  }

  @Test
  void aceitaBomEWindows1252() {
    byte[] comBom = "﻿Data;Valor\n1;2".getBytes(StandardCharsets.UTF_8);
    assertThat(tabela.linhas(comBom).getFirst()).containsExactly("Data", "Valor");
    byte[] legado = "Histórico;Tarifa de manutenção".getBytes(Charset.forName("windows-1252"));
    assertThat(tabela.linhas(legado).getFirst())
        .containsExactly("Histórico", "Tarifa de manutenção");
  }

  @Test
  void recusaVazioAspasAbertasEArquivoGrandeDemais() {
    assertThatThrownBy(() -> ler(" \n ; \n")).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> ler("a;\"b\n")).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> ler("a;b\n".repeat(5001)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("5.000");
  }
}
