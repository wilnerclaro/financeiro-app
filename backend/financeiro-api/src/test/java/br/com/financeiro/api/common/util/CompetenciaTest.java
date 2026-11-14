package br.com.financeiro.api.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financeiro.api.common.exception.BusinessException;
import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Competencia (ano e mês da API)")
class CompetenciaTest {

  @Test
  @DisplayName("aceita ano de 2000 a 2100 e mês de 1 a 12")
  void valida() {
    assertThat(Competencia.de(2026, 6)).isEqualTo(YearMonth.of(2026, 6));
    assertThat(Competencia.de(2000, 1)).isEqualTo(YearMonth.of(2000, 1));
    assertThat(Competencia.de(2100, 12)).isEqualTo(YearMonth.of(2100, 12));
  }

  @Test
  @DisplayName("recusa ano ou mês ausente ou fora do intervalo")
  void recusa() {
    assertThatThrownBy(() -> Competencia.de(null, 1)).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> Competencia.de(2026, null)).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> Competencia.de(1999, 1))
        .hasMessage("O ano deve estar entre 2000 e 2100.");
    assertThatThrownBy(() -> Competencia.de(2101, 1)).isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> Competencia.de(2026, 0)).hasMessage("O mês deve estar entre 1 e 12.");
    assertThatThrownBy(() -> Competencia.de(2026, 13)).isInstanceOf(BusinessException.class);
  }
}
