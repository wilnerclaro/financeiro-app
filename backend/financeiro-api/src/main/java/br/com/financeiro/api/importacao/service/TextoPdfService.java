package br.com.financeiro.api.importacao.service;

import br.com.financeiro.api.common.exception.BusinessException;
import java.io.IOException;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class TextoPdfService {
  static final int LIMITE_PAGINAS = 60;

  public List<String> linhas(byte[] arquivo) {
    try (PDDocument documento = Loader.loadPDF(arquivo)) {
      if (documento.getNumberOfPages() > LIMITE_PAGINAS) {
        throw new BusinessException("O PDF deve ter no máximo 60 páginas.");
      }
      PDFTextStripper extrator = new PDFTextStripper();
      extrator.setSortByPosition(true);
      List<String> linhas =
          extrator
              .getText(documento)
              .replace(' ', ' ')
              .lines()
              .map(String::strip)
              .filter(linha -> !linha.isEmpty())
              .toList();
      if (linhas.isEmpty()) {
        throw new BusinessException(
            "Este PDF não tem texto para ler: parece uma imagem ou um documento digitalizado."
                + " Baixe o extrato pelo app do banco e envie de novo.");
      }
      return linhas;
    } catch (InvalidPasswordException exception) {
      throw new BusinessException(
          "Este PDF está protegido por senha. Baixe o extrato sem senha pelo app do banco e envie"
              + " de novo.");
    } catch (IOException exception) {
      throw new BusinessException("Não foi possível ler o PDF. Envie o arquivo de novo.");
    }
  }
}
