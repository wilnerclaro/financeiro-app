package br.com.financeiro.api.importacao.service;

import br.com.financeiro.api.common.exception.BusinessException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class TabelaCsvService {
  static final int LIMITE_LINHAS = 5000;
  private static final char ASPAS = '"';

  public List<List<String>> linhas(byte[] arquivo) {
    String texto = decodificar(arquivo).replace("\r\n", "\n").replace('\r', '\n');
    List<List<String>> linhas = new Analise(texto, separador(texto)).linhas();
    if (linhas.isEmpty()) {
      throw new BusinessException("O CSV está vazio.");
    }
    return linhas;
  }

  private char separador(String texto) {
    String primeira =
        texto
            .lines()
            .filter(linha -> !linha.isBlank())
            .findFirst()
            .orElse("")
            .replaceAll("\"[^\"]*\"", "");
    return Stream.of(';', ',', '\t')
        .max(
            Comparator.comparingLong(
                candidato -> primeira.chars().filter(c -> c == candidato).count()))
        .orElse(';');
  }

  private String decodificar(byte[] arquivo) {
    int inicio =
        arquivo.length >= 3
                && arquivo[0] == (byte) 0xEF
                && arquivo[1] == (byte) 0xBB
                && arquivo[2] == (byte) 0xBF
            ? 3
            : 0;
    ByteBuffer conteudo = ByteBuffer.wrap(arquivo, inicio, arquivo.length - inicio);
    try {
      return estrito(StandardCharsets.UTF_8).decode(conteudo).toString();
    } catch (CharacterCodingException exception) {
      return new String(arquivo, inicio, arquivo.length - inicio, Charset.forName("windows-1252"));
    }
  }

  private CharsetDecoder estrito(Charset charset) {
    return charset
        .newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT);
  }

  private static final class Analise {
    private final String texto;
    private final char separador;
    private final List<List<String>> linhas = new ArrayList<>();
    private final StringBuilder celula = new StringBuilder();
    private List<String> linha = new ArrayList<>();
    private int posicao;

    private Analise(String texto, char separador) {
      this.texto = texto;
      this.separador = separador;
    }

    private List<List<String>> linhas() {
      while (posicao < texto.length()) {
        char atual = texto.charAt(posicao++);
        if (atual == ASPAS) {
          lerEntreAspas();
        } else if (atual == separador) {
          fecharCelula();
        } else if (atual == '\n') {
          fecharLinha();
        } else {
          celula.append(atual);
        }
      }
      fecharLinha();
      return List.copyOf(linhas);
    }

    private void lerEntreAspas() {
      while (posicao < texto.length()) {
        char atual = texto.charAt(posicao++);
        if (atual != ASPAS) {
          celula.append(atual);
        } else if (posicao < texto.length() && texto.charAt(posicao) == ASPAS) {
          celula.append(ASPAS);
          posicao++;
        } else {
          return;
        }
      }
      throw new BusinessException("O CSV tem aspas sem fechamento. Exporte o arquivo de novo.");
    }

    private void fecharCelula() {
      linha.add(celula.toString().strip());
      celula.setLength(0);
    }

    private void fecharLinha() {
      fecharCelula();
      if (linha.stream().anyMatch(valor -> !valor.isEmpty())) {
        if (linhas.size() >= LIMITE_LINHAS) {
          throw new BusinessException("O CSV deve ter no máximo 5.000 linhas.");
        }
        linhas.add(List.copyOf(linha));
      }
      linha = new ArrayList<>();
    }
  }
}
