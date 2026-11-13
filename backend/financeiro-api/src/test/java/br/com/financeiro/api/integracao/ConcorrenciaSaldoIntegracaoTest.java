package br.com.financeiro.api.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Saldo com gravações simultâneas")
class ConcorrenciaSaldoIntegracaoTest extends IntegracaoTest {

  private static final int REQUISICOES = 8;

  @Test
  @DisplayName("lançamentos e transferências ao mesmo tempo nas mesmas contas não perdem saldo")
  void naoPerdeSaldo() throws Exception {
    Sessao usuario = criarUsuario();
    UUID conta = criarConta(usuario, "Banco", "1000.00");
    UUID categoria = categoriaPadrao(usuario, "Supermercado");
    String corpo = corpoLancamento(conta, categoria, "DESPESA", "10.00", null, HOJE);

    CountDownLatch largada = new CountDownLatch(1);
    List<Callable<Integer>> tarefas = new ArrayList<>();
    for (int i = 0; i < REQUISICOES; i++) {
      tarefas.add(
          () -> {
            largada.await();
            return postar(usuario, "/api/lancamentos-financeiros", corpo)
                .andReturn()
                .getResponse()
                .getStatus();
          });
    }

    List<Integer> respostas = new ArrayList<>();
    try (ExecutorService executor = Executors.newFixedThreadPool(REQUISICOES)) {
      List<Future<Integer>> futuros = tarefas.stream().map(executor::submit).toList();
      largada.countDown();
      for (Future<Integer> futuro : futuros) {
        respostas.add(futuro.get());
      }
    }

    assertThat(respostas).containsOnly(201);
    assertThat(saldoAtual(conta, usuario)).isEqualByComparingTo("920.00");

    UUID poupanca = criarConta(usuario, "Poupança", "0.00");
    String transferencia =
        """
        {"contaOrigemId":"%s","contaDestinoId":"%s","valor":5.00,"dataTransferencia":"%s"}
        """;
    List<Callable<Integer>> idaEVolta = new ArrayList<>();
    for (int i = 0; i < REQUISICOES; i++) {
      boolean ida = i % 2 == 0;
      String corpoTransferencia =
          transferencia.formatted(ida ? conta : poupanca, ida ? poupanca : conta, HOJE);
      idaEVolta.add(
          () ->
              postar(usuario, "/api/transferencias", corpoTransferencia)
                  .andReturn()
                  .getResponse()
                  .getStatus());
    }
    try (ExecutorService executor = Executors.newFixedThreadPool(REQUISICOES)) {
      for (Future<Integer> futuro : executor.invokeAll(idaEVolta)) {
        assertThat(futuro.get()).isEqualTo(201);
      }
    }
    assertThat(saldoAtual(conta, usuario)).isEqualByComparingTo("920.00");
    assertThat(saldoAtual(poupanca, usuario)).isEqualByComparingTo("0.00");
  }
}
