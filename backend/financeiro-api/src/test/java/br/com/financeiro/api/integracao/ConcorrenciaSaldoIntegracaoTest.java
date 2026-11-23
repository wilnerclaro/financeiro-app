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

  @Test
  @DisplayName("pagar o mesmo lançamento várias vezes ao mesmo tempo debita uma vez só")
  void pagamentoSimultaneoDebitaUmaVez() throws Exception {
    Sessao usuario = criarUsuario();
    UUID conta = criarConta(usuario, "Banco", "1000.00");
    UUID categoria = categoriaPadrao(usuario, "Supermercado");
    UUID lancamento =
        idDe(criarLancamento(usuario, conta, categoria, "DESPESA", "100.00", HOJE, null));

    List<Integer> respostas =
        aoMesmoTempo(
            () ->
                alterar(
                        usuario,
                        "/api/lancamentos-financeiros/" + lancamento + "/pagar",
                        "{\"dataPagamento\":\"" + HOJE + "\"}")
                    .andReturn()
                    .getResponse()
                    .getStatus());

    assertThat(respostas).containsOnlyOnce(200).containsOnly(200, 400);
    assertThat(saldoAtual(conta, usuario)).isEqualByComparingTo("900.00");
  }

  @Test
  @DisplayName("cancelar o mesmo lançamento pago várias vezes ao mesmo tempo estorna uma vez só")
  void cancelamentoSimultaneoEstornaUmaVez() throws Exception {
    Sessao usuario = criarUsuario();
    UUID conta = criarConta(usuario, "Banco", "1000.00");
    UUID categoria = categoriaPadrao(usuario, "Supermercado");
    UUID lancamento =
        idDe(criarLancamento(usuario, conta, categoria, "DESPESA", "100.00", HOJE, HOJE));

    List<Integer> respostas =
        aoMesmoTempo(
            () ->
                remover("/api/lancamentos-financeiros/" + lancamento, usuario)
                    .andReturn()
                    .getResponse()
                    .getStatus());

    assertThat(respostas).containsOnlyOnce(204).containsOnly(204, 400);
    assertThat(saldoAtual(conta, usuario)).isEqualByComparingTo("1000.00");
  }

  @Test
  @DisplayName("cancelar a mesma transferência várias vezes ao mesmo tempo estorna uma vez só")
  void cancelamentoSimultaneoDeTransferencia() throws Exception {
    Sessao usuario = criarUsuario();
    UUID banco = criarConta(usuario, "Banco", "1000.00");
    UUID poupanca = criarConta(usuario, "Poupança", "0.00");
    UUID transferencia =
        idDe(
            postar(
                usuario,
                "/api/transferencias",
                """
                {"contaOrigemId":"%s","contaDestinoId":"%s","valor":50.00,"dataTransferencia":"%s"}
                """
                    .formatted(banco, poupanca, HOJE)));

    List<Integer> respostas =
        aoMesmoTempo(
            () ->
                remover("/api/transferencias/" + transferencia, usuario)
                    .andReturn()
                    .getResponse()
                    .getStatus());

    assertThat(respostas).containsOnlyOnce(204).containsOnly(204, 400);
    assertThat(saldoAtual(banco, usuario)).isEqualByComparingTo("1000.00");
    assertThat(saldoAtual(poupanca, usuario)).isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("pagar a mesma fatura várias vezes ao mesmo tempo debita uma vez só")
  void pagamentoSimultaneoDeFatura() throws Exception {
    Sessao usuario = criarUsuario();
    UUID banco = criarConta(usuario, "Banco", "1000.00");
    UUID cartao = criarCartao(usuario, "Cartão", 10, 20);
    UUID categoria = categoriaPadrao(usuario, "Supermercado");
    for (int i = 0; i < 3; i++) {
      criarLancamentoEm(usuario, cartao, categoria, "DESPESA", "100.00", HOJE, null, null);
    }
    String corpo =
        """
        {"contaId":"%s","ano":2026,"mes":7,"contaPagamentoId":"%s","dataPagamento":"%s"}
        """
            .formatted(cartao, banco, HOJE);

    List<Integer> respostas =
        aoMesmoTempo(
            () ->
                alterar(usuario, "/api/lancamentos-financeiros/faturas/pagar", corpo)
                    .andReturn()
                    .getResponse()
                    .getStatus());

    assertThat(respostas).containsOnlyOnce(200).containsOnly(200, 400);
    assertThat(saldoAtual(banco, usuario)).isEqualByComparingTo("700.00");
    assertThat(saldoAtual(cartao, usuario)).isEqualByComparingTo("0.00");
  }

  @Test
  @DisplayName("edições cruzadas que trocam lançamentos de conta não travam nem perdem saldo")
  void edicoesCruzadasDeConta() throws Exception {
    Sessao usuario = criarUsuario();
    UUID contaA = criarConta(usuario, "Conta A", "1000.00");
    UUID contaB = criarConta(usuario, "Conta B", "1000.00");
    UUID categoria = categoriaPadrao(usuario, "Supermercado");
    List<UUID> daContaA = new ArrayList<>();
    List<UUID> daContaB = new ArrayList<>();
    for (int i = 0; i < REQUISICOES / 2; i++) {
      daContaA.add(
          idDe(criarLancamento(usuario, contaA, categoria, "DESPESA", "10.00", HOJE, HOJE)));
      daContaB.add(
          idDe(criarLancamento(usuario, contaB, categoria, "DESPESA", "10.00", HOJE, HOJE)));
    }

    List<Callable<Integer>> tarefas = new ArrayList<>();
    for (int i = 0; i < REQUISICOES / 2; i++) {
      tarefas.add(moverPara(usuario, daContaA.get(i), contaB, categoria));
      tarefas.add(moverPara(usuario, daContaB.get(i), contaA, categoria));
    }
    try (ExecutorService executor = Executors.newFixedThreadPool(REQUISICOES)) {
      for (Future<Integer> futuro : executor.invokeAll(tarefas)) {
        assertThat(futuro.get()).isEqualTo(200);
      }
    }

    assertThat(saldoAtual(contaA, usuario)).isEqualByComparingTo("960.00");
    assertThat(saldoAtual(contaB, usuario)).isEqualByComparingTo("960.00");
  }

  private Callable<Integer> moverPara(Sessao usuario, UUID lancamento, UUID conta, UUID categoria) {
    String corpo = corpoLancamento(conta, categoria, "DESPESA", "10.00", HOJE, HOJE);
    return () ->
        atualizar(usuario, "/api/lancamentos-financeiros/" + lancamento, corpo)
            .andReturn()
            .getResponse()
            .getStatus();
  }

  private static List<Integer> aoMesmoTempo(Callable<Integer> tarefa) throws Exception {
    CountDownLatch largada = new CountDownLatch(1);
    List<Integer> respostas = new ArrayList<>();
    try (ExecutorService executor = Executors.newFixedThreadPool(REQUISICOES)) {
      List<Future<Integer>> futuros = new ArrayList<>();
      for (int i = 0; i < REQUISICOES; i++) {
        futuros.add(
            executor.submit(
                () -> {
                  largada.await();
                  return tarefa.call();
                }));
      }
      largada.countDown();
      for (Future<Integer> futuro : futuros) {
        respostas.add(futuro.get());
      }
    }
    return respostas;
  }
}
