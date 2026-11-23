package br.com.financeiro.api.importacao;

public final class OfxDeTeste {
  private OfxDeTeste() {}

  public static String documento(String transacoes) {
    return "OFXHEADER:100\nDATA:OFXSGML\nVERSION:102\nENCODING:UTF-8\n\n"
        + "<OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS><CURDEF>BRL\n"
        + "<BANKACCTFROM><BANKID>001\n<ACCTID>12345\n<ACCTTYPE>CHECKING\n</BANKACCTFROM>"
        + "<BANKTRANLIST>"
        + transacoes
        + "</BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>";
  }

  public static String transacao(String id, String valor) {
    return "<STMTTRN><TRNTYPE>DEBIT\n<DTPOSTED>20260610120000[-3:BRT]\n<TRNAMT>"
        + valor
        + "\n<FITID>"
        + id
        + "\n<NAME>Mercado\n<MEMO>Compra de café\n</STMTTRN>";
  }
}
