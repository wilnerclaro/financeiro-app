package br.com.financeiro.api.common.util;

import br.com.financeiro.api.common.exception.BusinessException;
import java.time.YearMonth;

public final class Competencia {

  public static final int MESES_NO_ANO = 12;
  private static final int PRIMEIRO_ANO = 2000;
  private static final int ULTIMO_ANO = 2100;

  private Competencia() {}

  public static YearMonth de(Integer ano, Integer mes) {
    if (ano == null || mes == null) {
      throw new BusinessException("Informe o ano e o mês.");
    }
    validarAno(ano);
    if (mes < 1 || mes > MESES_NO_ANO) {
      throw new BusinessException("O mês deve estar entre 1 e 12.");
    }
    return YearMonth.of(ano, mes);
  }

  public static int validarAno(int ano) {
    if (ano < PRIMEIRO_ANO || ano > ULTIMO_ANO) {
      throw new BusinessException(
          "O ano deve estar entre %d e %d.".formatted(PRIMEIRO_ANO, ULTIMO_ANO));
    }
    return ano;
  }
}
