package br.com.financeiro.api.importacao.service;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.util.NormalizadorTexto;
import br.com.financeiro.api.conta.entity.Conta;
import br.com.financeiro.api.conta.enums.TipoConta;
import br.com.financeiro.api.conta.service.ContaService;
import br.com.financeiro.api.importacao.dto.ConfirmacaoImportacaoResponse;
import br.com.financeiro.api.importacao.dto.ExtratoLido;
import br.com.financeiro.api.importacao.dto.ItemExtratoResponse;
import br.com.financeiro.api.importacao.dto.RevisaoExtratoResponse;
import br.com.financeiro.api.importacao.dto.SelecaoItemExtratoRequest;
import br.com.financeiro.api.importacao.dto.TransacaoLida;
import br.com.financeiro.api.importacao.entity.ItemImportado;
import br.com.financeiro.api.importacao.enums.FormatoExtrato;
import br.com.financeiro.api.importacao.repository.ItemImportadoRepository;
import br.com.financeiro.api.lancamentofinanceiro.dto.CriarLancamentoFinanceiroRequest;
import br.com.financeiro.api.lancamentofinanceiro.dto.LancamentoFinanceiroResponse;
import br.com.financeiro.api.lancamentofinanceiro.enums.StatusLancamentoFinanceiro;
import br.com.financeiro.api.lancamentofinanceiro.service.LancamentoFinanceiroService;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ImportacaoExtratoService {
  static final int LIMITE_BYTES = 2 * 1024 * 1024;
  static final int LIMITE_TRANSACOES = 1000;
  private static final int FOLGA_EM_DIAS = 45;

  private final LeitorOfxService leitorOfx;
  private final TextoPdfService textoPdf;
  private final TabelaCsvService tabelaCsv;
  private final ObjectProvider<LeitorDeExtratoPdf> leitoresPdf;
  private final ObjectProvider<LeitorDeExtratoCsv> leitoresCsv;
  private final ContaService contaService;
  private final LancamentoFinanceiroService lancamentoService;
  private final ItemImportadoRepository repository;
  private final ObjectMapper objectMapper;

  @Transactional(readOnly = true)
  public RevisaoExtratoResponse revisar(UUID usuarioId, UUID contaId, MultipartFile arquivo) {
    buscarConta(usuarioId, contaId);
    ExtratoLido extrato = ler(arquivo);
    List<TransacaoLida> transacoes = extrato.transacoes();
    Set<String> jaImportados = new HashSet<>();
    Map<String, Integer> semPar = lancamentosDaConta(usuarioId, contaId, transacoes);
    for (TransacaoLida transacao : transacoes) {
      if (repository.existsByUsuarioIdAndContaIdAndIdentificador(
          usuarioId, contaId, transacao.identificador())) {
        jaImportados.add(transacao.identificador());
        consumir(semPar, chave(transacao));
      }
    }
    List<ItemExtratoResponse> itens = new ArrayList<>();
    for (TransacaoLida transacao : transacoes) {
      boolean jaImportado = jaImportados.contains(transacao.identificador());
      boolean possivelDuplicado =
          !jaImportado && transacao.importavel() && consumir(semPar, chave(transacao));
      itens.add(
          new ItemExtratoResponse(
              transacao.identificador(),
              transacao.data(),
              transacao.descricao(),
              transacao.valor(),
              transacao.tipo(),
              jaImportado,
              possivelDuplicado,
              transacao.importavel(),
              transacao.motivo()));
    }
    return new RevisaoExtratoResponse(extrato.formato(), extrato.banco(), itens, "BRL");
  }

  @Transactional
  public ConfirmacaoImportacaoResponse confirmar(
      UUID usuarioId, UUID contaId, MultipartFile arquivo, String selecoesJson) {
    Conta conta = buscarConta(usuarioId, contaId);
    ExtratoLido extrato = ler(arquivo);
    List<SelecaoItemExtratoRequest> selecoes = selecoes(selecoesJson);
    Map<String, TransacaoLida> porId =
        extrato.transacoes().stream()
            .collect(Collectors.toMap(TransacaoLida::identificador, Function.identity()));
    Set<String> escolhidos = new HashSet<>();
    for (SelecaoItemExtratoRequest selecao : selecoes) {
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
    for (SelecaoItemExtratoRequest selecao : selecoes) {
      TransacaoLida transacao = porId.get(selecao.identificador());
      if (repository.existsByUsuarioIdAndContaIdAndIdentificador(
          usuarioId, contaId, transacao.identificador())) {
        continue;
      }
      LancamentoFinanceiroResponse lancamento =
          lancamentoService.criar(
              usuarioId,
              new CriarLancamentoFinanceiroRequest(
                  contaId,
                  selecao.categoriaId(),
                  transacao.descricao(),
                  transacao.tipo(),
                  transacao.valor(),
                  transacao.data(),
                  transacao.data(),
                  transacao.data(),
                  "Importado de extrato " + extrato.formato()));
      repository.saveAndFlush(
          ItemImportado.builder()
              .usuarioId(usuarioId)
              .contaId(contaId)
              .identificador(transacao.identificador())
              .formato(extrato.formato())
              .lancamentoId(lancamento.id())
              .build());
      importados++;
    }
    return new ConfirmacaoImportacaoResponse(importados, extrato.transacoes().size() - importados);
  }

  private Conta buscarConta(UUID usuarioId, UUID contaId) {
    Conta conta = contaService.buscarContaAtiva(contaId, usuarioId);
    validarTipo(conta);
    return conta;
  }

  private void validarTipo(Conta conta) {
    if (conta.getTipo() != TipoConta.CONTA_CORRENTE && conta.getTipo() != TipoConta.POUPANCA) {
      throw new BusinessException(
          "Escolha uma conta corrente ou poupança para importar o extrato.");
    }
  }

  private ExtratoLido ler(MultipartFile arquivo) {
    if (arquivo.isEmpty() || arquivo.getSize() > LIMITE_BYTES) {
      throw new BusinessException("Envie um arquivo de extrato com conteúdo e até 2 MB.");
    }
    byte[] conteudo;
    try {
      conteudo = arquivo.getBytes();
    } catch (IOException exception) {
      throw new BusinessException("Não foi possível ler o arquivo. Envie novamente.");
    }
    ExtratoLido extrato =
        switch (formatoDe(arquivo.getOriginalFilename(), conteudo)) {
          case OFX -> new ExtratoLido(FormatoExtrato.OFX, null, leitorOfx.ler(conteudo));
          case PDF -> lerPdf(conteudo);
          case CSV -> lerCsv(conteudo);
        };
    if (extrato.transacoes().isEmpty()) {
      throw new BusinessException("O extrato não tem transações para importar.");
    }
    if (extrato.transacoes().size() > LIMITE_TRANSACOES) {
      throw new BusinessException("O arquivo deve ter no máximo 1.000 transações.");
    }
    return extrato;
  }

  private FormatoExtrato formatoDe(String nomeDoArquivo, byte[] conteudo) {
    String inicio =
        new String(conteudo, 0, Math.min(conteudo.length, 1024), StandardCharsets.ISO_8859_1)
            .toUpperCase(Locale.ROOT);
    String nome = Objects.requireNonNullElse(nomeDoArquivo, "").toLowerCase(Locale.ROOT);
    if (inicio.startsWith("%PDF-")) {
      return FormatoExtrato.PDF;
    }
    if (inicio.contains("OFXHEADER") || inicio.contains("<OFX") || nome.endsWith(".ofx")) {
      return FormatoExtrato.OFX;
    }
    if (nome.endsWith(".csv")) {
      return FormatoExtrato.CSV;
    }
    throw new BusinessException("Formato não reconhecido. Envie o extrato em OFX, CSV ou PDF.");
  }

  private ExtratoLido lerPdf(byte[] conteudo) {
    List<String> linhas = textoPdf.linhas(conteudo);
    LeitorDeExtratoPdf leitor =
        leitoresPdf
            .orderedStream()
            .filter(candidato -> candidato.reconhece(linhas))
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessException(
                        "Ainda não sabemos ler extratos em PDF deste banco. Se o banco oferecer"
                            + " OFX, use esse formato."));
    return new ExtratoLido(
        FormatoExtrato.PDF, leitor.banco(), identificar(leitor.banco(), leitor.ler(linhas)));
  }

  private ExtratoLido lerCsv(byte[] conteudo) {
    List<List<String>> linhas = tabelaCsv.linhas(conteudo);
    LeitorDeExtratoCsv leitor =
        leitoresCsv
            .orderedStream()
            .filter(candidato -> candidato.reconhece(linhas))
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessException(
                        "Ainda não sabemos ler o CSV deste banco. Se o banco oferecer OFX, use"
                            + " esse formato."));
    return new ExtratoLido(
        FormatoExtrato.CSV, leitor.banco(), identificar(leitor.banco(), leitor.ler(linhas)));
  }

  private List<TransacaoLida> identificar(String banco, List<TransacaoLida> transacoes) {
    Map<String, Integer> ocorrencias = new HashMap<>();
    return transacoes.stream()
        .map(
            transacao -> {
              String base =
                  String.join(
                      "\u0000",
                      banco,
                      chave(transacao),
                      NormalizadorTexto.normalizarParaComparacao(transacao.descricao()));
              int ordem = ocorrencias.merge(base, 1, Integer::sum);
              return transacao.comIdentificador(hash(base + "\u0000" + ordem));
            })
        .toList();
  }

  private Map<String, Integer> lancamentosDaConta(
      UUID usuarioId, UUID contaId, List<TransacaoLida> transacoes) {
    LocalDate inicio =
        transacoes.stream().map(TransacaoLida::data).min(Comparator.naturalOrder()).orElseThrow();
    LocalDate fim =
        transacoes.stream().map(TransacaoLida::data).max(Comparator.naturalOrder()).orElseThrow();
    Map<String, Integer> contagem = new HashMap<>();
    lancamentoService
        .listar(
            usuarioId,
            null,
            null,
            contaId,
            null,
            inicio.minusDays(FOLGA_EM_DIAS),
            fim.plusDays(FOLGA_EM_DIAS),
            Pageable.unpaged())
        .stream()
        .filter(lancamento -> lancamento.status() != StatusLancamentoFinanceiro.CANCELADO)
        .forEach(lancamento -> contagem.merge(chave(lancamento), 1, Integer::sum));
    return contagem;
  }

  private boolean consumir(Map<String, Integer> contagem, String chave) {
    Integer restante = contagem.get(chave);
    if (restante == null || restante == 0) {
      return false;
    }
    contagem.put(chave, restante - 1);
    return true;
  }

  private String chave(TransacaoLida transacao) {
    return chave(transacao.data(), transacao.tipo().name(), transacao.valor());
  }

  private String chave(LancamentoFinanceiroResponse lancamento) {
    LocalDate data =
        Objects.requireNonNullElse(
            lancamento.dataPagamento(),
            Objects.requireNonNullElse(lancamento.dataVencimento(), lancamento.dataCompetencia()));
    return chave(data, lancamento.tipo().name(), lancamento.valor());
  }

  private String chave(LocalDate data, String tipo, BigDecimal valor) {
    return data + "|" + tipo + "|" + valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private List<SelecaoItemExtratoRequest> selecoes(String json) {
    if (json.length() > 200000) {
      throw new BusinessException("Seleção de transações muito grande.");
    }
    SelecaoItemExtratoRequest[] selecoes;
    try {
      selecoes = objectMapper.readValue(json, SelecaoItemExtratoRequest[].class);
    } catch (RuntimeException exception) {
      throw new BusinessException("A seleção de transações está em formato inválido.");
    }
    if (selecoes == null || selecoes.length == 0 || selecoes.length > LIMITE_TRANSACOES) {
      throw new BusinessException("Selecione entre 1 e 1.000 transações para importar.");
    }
    return Arrays.asList(selecoes);
  }

  private String hash(String texto) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
