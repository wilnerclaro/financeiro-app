package br.com.financeiro.api.importacao.service;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.conta.entity.Conta;
import br.com.financeiro.api.conta.enums.TipoConta;
import br.com.financeiro.api.conta.service.ContaService;
import br.com.financeiro.api.importacao.dto.ConfirmacaoOfxResponse;
import br.com.financeiro.api.importacao.dto.ItemOfxResponse;
import br.com.financeiro.api.importacao.dto.RevisaoOfxResponse;
import br.com.financeiro.api.importacao.dto.SelecaoOfxRequest;
import br.com.financeiro.api.importacao.entity.ItemImportado;
import br.com.financeiro.api.importacao.repository.ItemImportadoRepository;
import br.com.financeiro.api.lancamentofinanceiro.dto.CriarLancamentoFinanceiroRequest;
import br.com.financeiro.api.lancamentofinanceiro.dto.LancamentoFinanceiroResponse;
import br.com.financeiro.api.lancamentofinanceiro.service.LancamentoFinanceiroService;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ImportacaoOfxService {
  private final LeitorOfxService leitor;
  private final ContaService contaService;
  private final LancamentoFinanceiroService lancamentoService;
  private final ItemImportadoRepository repository;
  private final ObjectMapper objectMapper;

  @Transactional(readOnly = true)
  public RevisaoOfxResponse revisar(UUID usuarioId, UUID contaId, MultipartFile arquivo) {
    buscarConta(usuarioId, contaId);
    List<ItemOfxResponse> itens =
        ler(arquivo).stream()
            .map(
                item -> {
                  boolean jaImportado =
                      repository.existsByUsuarioIdAndContaIdAndIdentificador(
                          usuarioId, contaId, item.identificador());
                  return new ItemOfxResponse(
                      item.identificador(),
                      item.data(),
                      item.descricao(),
                      item.valor(),
                      item.tipo(),
                      jaImportado,
                      item.importavel(),
                      item.motivo());
                })
            .toList();
    return new RevisaoOfxResponse(itens, "BRL");
  }

  @Transactional
  public ConfirmacaoOfxResponse confirmar(
      UUID usuarioId, UUID contaId, MultipartFile arquivo, String selecoesJson) {
    Conta conta = buscarConta(usuarioId, contaId);
    List<ItemOfxResponse> itens = ler(arquivo);
    List<SelecaoOfxRequest> selecoes = selecoes(selecoesJson);
    Map<String, ItemOfxResponse> porId =
        itens.stream()
            .collect(Collectors.toMap(ItemOfxResponse::identificador, Function.identity()));
    Set<String> escolhidos = new HashSet<>();
    for (SelecaoOfxRequest selecao : selecoes) {
      if (selecao == null
          || selecao.categoriaId() == null
          || !porId.containsKey(selecao.identificador())
          || !escolhidos.add(selecao.identificador())
          || !porId.get(selecao.identificador()).importavel()) {
        throw new BusinessException(
            "Revise os itens selecionados e informe uma categoria para cada transação importável.");
      }
    }
    contaService.travar(conta);
    if (!Boolean.TRUE.equals(conta.getAtiva())) {
      throw new BusinessException("A conta informada está inativa.");
    }
    validarTipo(conta);
    int importados = 0;
    for (SelecaoOfxRequest selecao : selecoes) {
      ItemOfxResponse item = porId.get(selecao.identificador());
      if (repository.existsByUsuarioIdAndContaIdAndIdentificador(
          usuarioId, contaId, item.identificador())) {
        continue;
      }
      LancamentoFinanceiroResponse lancamento =
          lancamentoService.criar(
              usuarioId,
              new CriarLancamentoFinanceiroRequest(
                  contaId,
                  selecao.categoriaId(),
                  item.descricao(),
                  item.tipo(),
                  item.valor(),
                  item.data(),
                  item.data(),
                  item.data(),
                  "Importado de extrato OFX"));
      repository.saveAndFlush(
          ItemImportado.builder()
              .usuarioId(usuarioId)
              .contaId(contaId)
              .identificador(item.identificador())
              .lancamentoId(lancamento.id())
              .build());
      importados++;
    }
    return new ConfirmacaoOfxResponse(importados, itens.size() - importados);
  }

  private Conta buscarConta(UUID usuarioId, UUID contaId) {
    Conta conta = contaService.buscarContaAtiva(contaId, usuarioId);
    validarTipo(conta);
    return conta;
  }

  private void validarTipo(Conta conta) {
    if (conta.getTipo() != TipoConta.CONTA_CORRENTE && conta.getTipo() != TipoConta.POUPANCA) {
      throw new BusinessException(
          "Escolha uma conta corrente ou poupança para importar o extrato OFX.");
    }
  }

  private List<ItemOfxResponse> ler(MultipartFile arquivo) {
    if (arquivo.getSize() > LeitorOfxService.LIMITE_BYTES) {
      throw new BusinessException("Envie um arquivo OFX de até 2 MB.");
    }
    try {
      return leitor.ler(arquivo.getBytes());
    } catch (IOException exception) {
      throw new BusinessException("Não foi possível ler o arquivo OFX. Envie novamente.");
    }
  }

  private List<SelecaoOfxRequest> selecoes(String json) {
    if (json.length() > 200000) {
      throw new BusinessException("Seleção de transações muito grande.");
    }
    SelecaoOfxRequest[] selecoes;
    try {
      selecoes = objectMapper.readValue(json, SelecaoOfxRequest[].class);
    } catch (RuntimeException exception) {
      throw new BusinessException("A seleção de transações está em formato inválido.");
    }
    if (selecoes == null || selecoes.length == 0 || selecoes.length > 1000) {
      throw new BusinessException("Selecione entre 1 e 1.000 transações para importar.");
    }
    return Arrays.asList(selecoes);
  }
}
