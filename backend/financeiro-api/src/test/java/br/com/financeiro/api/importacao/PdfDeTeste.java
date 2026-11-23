package br.com.financeiro.api.importacao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public final class PdfDeTeste {
  private PdfDeTeste() {}

  public static byte[] pdf(String... linhas) {
    return gerar(null, 1, linhas);
  }

  public static byte[] comSenha(String... linhas) {
    return gerar("1234", 1, linhas);
  }

  public static byte[] comPaginas(int paginas) {
    return gerar(null, paginas, "Página");
  }

  private static byte[] gerar(String senha, int paginas, String... linhas) {
    try (PDDocument documento = new PDDocument();
        ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
      PDType1Font fonte = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
      for (int pagina = 0; pagina < paginas; pagina++) {
        PDPage folha = new PDPage();
        documento.addPage(folha);
        try (PDPageContentStream conteudo = new PDPageContentStream(documento, folha)) {
          float altura = 750;
          for (String linha : linhas) {
            conteudo.beginText();
            conteudo.setFont(fonte, 11);
            conteudo.newLineAtOffset(50, altura);
            conteudo.showText(linha);
            conteudo.endText();
            altura -= 16;
          }
        }
      }
      if (senha != null) {
        StandardProtectionPolicy politica =
            new StandardProtectionPolicy(senha, senha, new AccessPermission());
        politica.setEncryptionKeyLength(128);
        documento.protect(politica);
      }
      documento.save(saida);
      return saida.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
