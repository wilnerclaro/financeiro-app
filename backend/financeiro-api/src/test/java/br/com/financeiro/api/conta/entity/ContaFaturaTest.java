package br.com.financeiro.api.conta.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Fatura do cartão de crédito")
class ContaFaturaTest {

  private static Conta cartao(int fechamento, int vencimento) {
    Conta conta = new Conta();
    conta.setDiaFechamento(fechamento);
    conta.setDiaVencimento(vencimento);
    return conta;
  }

  @Test
  @DisplayName("vencimento depois do fechamento: vence no mesmo mês em que fecha")
  void venceNoMesDoFechamento() {
    Conta conta = cartao(10, 20);
    assertThat(conta.vencimentoDaFatura(LocalDate.of(2026, 6, 10)))
        .isEqualTo(LocalDate.of(2026, 6, 20));
    assertThat(conta.vencimentoDaFatura(LocalDate.of(2026, 6, 11)))
        .isEqualTo(LocalDate.of(2026, 7, 20));
    assertThat(conta.fechamentoDaFatura(YearMonth.of(2026, 7)))
        .isEqualTo(LocalDate.of(2026, 7, 10));
  }

  @Test
  @DisplayName("vencimento antes do fechamento: vence no mês seguinte ao fechamento")
  void venceNoMesSeguinte() {
    Conta conta = cartao(25, 5);
    assertThat(conta.vencimentoDaFatura(LocalDate.of(2026, 6, 20)))
        .isEqualTo(LocalDate.of(2026, 7, 5));
    assertThat(conta.vencimentoDaFatura(LocalDate.of(2026, 6, 26)))
        .isEqualTo(LocalDate.of(2026, 8, 5));
    assertThat(conta.fechamentoDaFatura(YearMonth.of(2026, 8)))
        .isEqualTo(LocalDate.of(2026, 7, 25));
  }

  @Test
  @DisplayName("compra em dezembro depois do fechamento vira fatura de janeiro do ano seguinte")
  void viradaDeAno() {
    assertThat(cartao(10, 20).vencimentoDaFatura(LocalDate.of(2026, 12, 15)))
        .isEqualTo(LocalDate.of(2027, 1, 20));
  }
}
