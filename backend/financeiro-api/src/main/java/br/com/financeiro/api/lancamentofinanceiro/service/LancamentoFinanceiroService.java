package br.com.financeiro.api.lancamentofinanceiro.service;

import static br.com.financeiro.api.lancamentofinanceiro.repository.LancamentoFinanceiroSpecifications.comStatus;
import static br.com.financeiro.api.lancamentofinanceiro.repository.LancamentoFinanceiroSpecifications.competenciaEntre;
import static br.com.financeiro.api.lancamentofinanceiro.repository.LancamentoFinanceiroSpecifications.daCategoria;
import static br.com.financeiro.api.lancamentofinanceiro.repository.LancamentoFinanceiroSpecifications.daConta;
import static br.com.financeiro.api.lancamentofinanceiro.repository.LancamentoFinanceiroSpecifications.doTipo;
import static br.com.financeiro.api.lancamentofinanceiro.repository.LancamentoFinanceiroSpecifications.doUsuario;

import br.com.financeiro.api.categoria.entity.Categoria;
import br.com.financeiro.api.categoria.service.CategoriaService;
import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.exception.ResourceNotFoundException;
import br.com.financeiro.api.common.util.Competencia;
import br.com.financeiro.api.common.util.Dinheiro;
import br.com.financeiro.api.common.util.NormalizadorTexto;
import br.com.financeiro.api.conta.entity.Conta;
import br.com.financeiro.api.conta.service.ContaService;
import br.com.financeiro.api.lancamentofinanceiro.dto.AtualizarLancamentoFinanceiroRequest;
import br.com.financeiro.api.lancamentofinanceiro.dto.CriarLancamentoFinanceiroRequest;
import br.com.financeiro.api.lancamentofinanceiro.dto.FaturaCartaoResponse;
import br.com.financeiro.api.lancamentofinanceiro.dto.LancamentoFinanceiroResponse;
import br.com.financeiro.api.lancamentofinanceiro.dto.LancamentoRapidoResponse;
import br.com.financeiro.api.lancamentofinanceiro.dto.PagarFaturaRequest;
import br.com.financeiro.api.lancamentofinanceiro.dto.PagarLancamentoFinanceiroRequest;
import br.com.financeiro.api.lancamentofinanceiro.entity.LancamentoFinanceiro;
import br.com.financeiro.api.lancamentofinanceiro.enums.StatusLancamentoFinanceiro;
import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import br.com.financeiro.api.lancamentofinanceiro.mapper.LancamentoFinanceiroMapper;
import br.com.financeiro.api.lancamentofinanceiro.repository.LancamentoFinanceiroRepository;
import br.com.financeiro.api.transferencia.dto.CriarTransferenciaRequest;
import br.com.financeiro.api.transferencia.service.TransferenciaService;
import br.com.financeiro.api.usuario.entity.Usuario;
import br.com.financeiro.api.usuario.service.UsuarioService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LancamentoFinanceiroService {

  private static final int DIAS_DAS_CATEGORIAS_MAIS_USADAS = 90;
  private static final int LIMITE_DE_CATEGORIAS_MAIS_USADAS = 6;

  private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
  private final UsuarioService usuarioService;
  private final ContaService contaService;
  private final CategoriaService categoriaService;
  private final TransferenciaService transferenciaService;
  private final LancamentoFinanceiroMapper lancamentoFinanceiroMapper;
  private final Clock clock;

  @Transactional
  public LancamentoFinanceiroResponse criar(
      UUID usuarioId, CriarLancamentoFinanceiroRequest request) {
    Usuario usuario = usuarioService.buscar(usuarioId);

    LancamentoFinanceiro lancamentoFinanceiro = new LancamentoFinanceiro();
    lancamentoFinanceiro.setUsuario(usuario);
    preencher(lancamentoFinanceiro, DadosDoLancamento.de(request), usuarioId);

    movimentarSaldoSePago(lancamentoFinanceiro);

    return paraResponse(lancamentoFinanceiroRepository.saveAndFlush(lancamentoFinanceiro));
  }

  @Transactional(readOnly = true)
  public Page<LancamentoFinanceiroResponse> listar(
      UUID usuarioId,
      TipoLancamentoFinanceiro tipo,
      StatusLancamentoFinanceiro status,
      UUID contaId,
      UUID categoriaId,
      LocalDate dataInicio,
      LocalDate dataFim,
      Pageable pageable) {
    usuarioService.buscar(usuarioId);
    validarPeriodo(dataInicio, dataFim);
    contaService.validarContaDoUsuario(contaId, usuarioId);
    categoriaService.validarCategoriaDoUsuario(categoriaId, usuarioId);

    Specification<LancamentoFinanceiro> filtros =
        Specification.allOf(
            doUsuario(usuarioId),
            doTipo(tipo),
            comStatus(status, hoje()),
            daConta(contaId),
            daCategoria(categoriaId),
            competenciaEntre(dataInicio, dataFim));

    return lancamentoFinanceiroRepository.findAll(filtros, pageable).map(this::paraResponse);
  }

  @Transactional(readOnly = true)
  public LancamentoFinanceiroResponse buscarPorId(UUID id, UUID usuarioId) {
    return paraResponse(buscarLancamentoDoUsuario(id, usuarioId));
  }

  @Transactional
  public LancamentoFinanceiroResponse atualizar(
      UUID id, UUID usuarioId, AtualizarLancamentoFinanceiroRequest request) {
    LancamentoFinanceiro lancamentoFinanceiro = travarLancamentoDoUsuario(id, usuarioId);

    if (StatusLancamentoFinanceiro.CANCELADO.equals(lancamentoFinanceiro.getStatus())) {
      throw new BusinessException("Não é possível editar um lançamento cancelado.");
    }

    contaService.travar(
        lancamentoFinanceiro.getConta(),
        contaService.buscarContaAtiva(request.contaId(), usuarioId));
    estornarSaldoSePago(lancamentoFinanceiro);
    preencher(lancamentoFinanceiro, DadosDoLancamento.de(request), usuarioId);
    movimentarSaldoSePago(lancamentoFinanceiro);
    lancamentoFinanceiroRepository.flush();

    return paraResponse(lancamentoFinanceiro);
  }

  @Transactional
  public LancamentoFinanceiroResponse pagar(
      UUID id, UUID usuarioId, PagarLancamentoFinanceiroRequest request) {
    LancamentoFinanceiro lancamentoFinanceiro = travarLancamentoDoUsuario(id, usuarioId);

    if (StatusLancamentoFinanceiro.CANCELADO.equals(lancamentoFinanceiro.getStatus())) {
      throw new BusinessException("Não é possível pagar um lançamento cancelado.");
    }

    if (StatusLancamentoFinanceiro.PAGO.equals(lancamentoFinanceiro.getStatus())) {
      throw new BusinessException("O lançamento informado já está pago.");
    }

    validarDataPagamento(request.dataPagamento());

    lancamentoFinanceiro.setDataPagamento(request.dataPagamento());
    lancamentoFinanceiro.setStatus(StatusLancamentoFinanceiro.PAGO);
    movimentarSaldoSePago(lancamentoFinanceiro);
    lancamentoFinanceiroRepository.flush();

    return paraResponse(lancamentoFinanceiro);
  }

  @Transactional
  public void cancelar(UUID id, UUID usuarioId) {
    LancamentoFinanceiro lancamentoFinanceiro = travarLancamentoDoUsuario(id, usuarioId);

    if (StatusLancamentoFinanceiro.CANCELADO.equals(lancamentoFinanceiro.getStatus())) {
      throw new BusinessException("O lançamento informado já está cancelado.");
    }

    estornarSaldoSePago(lancamentoFinanceiro);
    lancamentoFinanceiro.setDataPagamento(null);
    lancamentoFinanceiro.setStatus(StatusLancamentoFinanceiro.CANCELADO);
  }

  @Transactional(readOnly = true)
  public FaturaCartaoResponse buscarFatura(UUID usuarioId, UUID contaId, Integer ano, Integer mes) {
    return montarFatura(buscarCartaoDoUsuario(contaId, usuarioId), usuarioId, ano, mes);
  }

  @Transactional
  public FaturaCartaoResponse pagarFatura(UUID usuarioId, PagarFaturaRequest request) {
    Conta cartao = buscarCartaoDoUsuario(request.contaId(), usuarioId);
    YearMonth mesDoVencimento = Competencia.de(request.ano(), request.mes());
    validarDataPagamento(request.dataPagamento());

    List<LancamentoFinanceiro> pendentes =
        lancamentoFinanceiroRepository.travarPendentesDaFatura(
            usuarioId, cartao.getId(), mesDoVencimento.atDay(1), mesDoVencimento.atEndOfMonth());

    if (pendentes.isEmpty()) {
      throw new BusinessException("Não há compras pendentes nesta fatura.");
    }

    BigDecimal valorAPagar = somarFatura(pendentes);
    if (valorAPagar.signum() > 0) {
      transferenciaService.criar(
          usuarioId,
          new CriarTransferenciaRequest(
              request.contaPagamentoId(),
              cartao.getId(),
              valorAPagar,
              request.dataPagamento(),
              "Pagamento da fatura %s %02d/%d"
                  .formatted(cartao.getNome(), request.mes(), request.ano())));
    }

    for (LancamentoFinanceiro lancamento : pendentes) {
      lancamento.setDataPagamento(request.dataPagamento());
      lancamento.setStatus(StatusLancamentoFinanceiro.PAGO);
      movimentarSaldoSePago(lancamento);
    }
    lancamentoFinanceiroRepository.flush();

    return montarFatura(cartao, usuarioId, request.ano(), request.mes());
  }

  @Transactional(readOnly = true)
  public List<LancamentoFinanceiroResponse> listarAPagarAte(UUID usuarioId, LocalDate ate) {
    return lancamentoFinanceiroRepository.buscarAPagarAte(usuarioId, ate).stream()
        .map(this::paraResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<LancamentoFinanceiroResponse> listarUltimos(UUID usuarioId, int quantidade) {
    return lancamentoFinanceiroRepository
        .buscarUltimos(usuarioId, PageRequest.of(0, quantidade))
        .stream()
        .map(this::paraResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public LancamentoRapidoResponse sugerirLancamentoRapido(UUID usuarioId) {
    Conta conta =
        lancamentoFinanceiroRepository
            .buscarMaisRecentesEmContaAtiva(usuarioId, PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .map(LancamentoFinanceiro::getConta)
            .orElse(null);

    List<LancamentoRapidoResponse.Categoria> categorias =
        lancamentoFinanceiroRepository
            .buscarCategoriasMaisUsadas(
                usuarioId,
                TipoLancamentoFinanceiro.DESPESA,
                OffsetDateTime.now(clock).minusDays(DIAS_DAS_CATEGORIAS_MAIS_USADAS),
                PageRequest.of(0, LIMITE_DE_CATEGORIAS_MAIS_USADAS))
            .stream()
            .map(
                c ->
                    new LancamentoRapidoResponse.Categoria(
                        c.getCategoriaId(), c.getNome(), c.getQuantidade()))
            .toList();

    return new LancamentoRapidoResponse(
        conta == null ? null : conta.getId(), conta == null ? null : conta.getNome(), categorias);
  }

  private record DadosDoLancamento(
      UUID contaId,
      UUID categoriaId,
      String descricao,
      TipoLancamentoFinanceiro tipo,
      BigDecimal valor,
      LocalDate dataCompetencia,
      LocalDate dataVencimento,
      LocalDate dataPagamento,
      String observacao) {

    static DadosDoLancamento de(CriarLancamentoFinanceiroRequest request) {
      return new DadosDoLancamento(
          request.contaId(),
          request.categoriaId(),
          request.descricao(),
          request.tipo(),
          request.valor(),
          request.dataCompetencia(),
          request.dataVencimento(),
          request.dataPagamento(),
          request.observacao());
    }

    static DadosDoLancamento de(AtualizarLancamentoFinanceiroRequest request) {
      return new DadosDoLancamento(
          request.contaId(),
          request.categoriaId(),
          request.descricao(),
          request.tipo(),
          request.valor(),
          request.dataCompetencia(),
          request.dataVencimento(),
          request.dataPagamento(),
          request.observacao());
    }
  }

  private void preencher(
      LancamentoFinanceiro lancamentoFinanceiro, DadosDoLancamento dados, UUID usuarioId) {
    Conta conta = contaService.buscarContaAtiva(dados.contaId(), usuarioId);
    Categoria categoria = categoriaService.buscarCategoriaAtiva(dados.categoriaId(), usuarioId);

    validarCategoriaCompativelComTipo(categoria, dados.tipo());
    validarDataPagamento(dados.dataPagamento());

    LocalDate dataCompetencia = dados.dataCompetencia();
    LocalDate dataVencimento = dados.dataVencimento();
    LocalDate dataCompra = null;

    if (conta.ehCartaoDeCredito()) {
      dataCompra = dados.dataCompetencia();
      dataVencimento = conta.vencimentoDaFatura(dataCompra);
      dataCompetencia = dataVencimento;
    }

    lancamentoFinanceiro.setConta(conta);
    lancamentoFinanceiro.setCategoria(categoria);
    lancamentoFinanceiro.setDescricao(NormalizadorTexto.normalizarNome(dados.descricao()));
    lancamentoFinanceiro.setTipo(dados.tipo());
    lancamentoFinanceiro.setValor(dados.valor());
    lancamentoFinanceiro.setDataCompetencia(dataCompetencia);
    lancamentoFinanceiro.setDataVencimento(dataVencimento);
    lancamentoFinanceiro.setDataCompra(dataCompra);
    lancamentoFinanceiro.setDataPagamento(dados.dataPagamento());
    lancamentoFinanceiro.setStatus(
        dados.dataPagamento() != null
            ? StatusLancamentoFinanceiro.PAGO
            : StatusLancamentoFinanceiro.PENDENTE);
    lancamentoFinanceiro.setObservacao(dados.observacao());
  }

  private void movimentarSaldoSePago(LancamentoFinanceiro lancamento) {
    if (!StatusLancamentoFinanceiro.PAGO.equals(lancamento.getStatus())) {
      return;
    }

    if (TipoLancamentoFinanceiro.RECEITA.equals(lancamento.getTipo())) {
      contaService.creditar(lancamento.getConta(), lancamento.getValor());
    } else {
      contaService.debitar(lancamento.getConta(), lancamento.getValor());
    }
  }

  private void estornarSaldoSePago(LancamentoFinanceiro lancamento) {
    if (!StatusLancamentoFinanceiro.PAGO.equals(lancamento.getStatus())) {
      return;
    }

    if (TipoLancamentoFinanceiro.RECEITA.equals(lancamento.getTipo())) {
      contaService.debitar(lancamento.getConta(), lancamento.getValor());
    } else {
      contaService.creditar(lancamento.getConta(), lancamento.getValor());
    }
  }

  private LancamentoFinanceiroResponse paraResponse(LancamentoFinanceiro lancamentoFinanceiro) {
    return lancamentoFinanceiroMapper.paraResponse(lancamentoFinanceiro, hoje());
  }

  private LocalDate hoje() {
    return LocalDate.now(clock);
  }

  private void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
    if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
      throw new BusinessException("A data inicial não pode ser posterior à data final.");
    }
  }

  private void validarDataPagamento(LocalDate dataPagamento) {
    if (dataPagamento != null && dataPagamento.isAfter(hoje())) {
      throw new BusinessException("A data de pagamento não pode ser futura.");
    }
  }

  private void validarCategoriaCompativelComTipo(
      Categoria categoria, TipoLancamentoFinanceiro tipoLancamento) {
    if (!tipoLancamento.aceitaCategoria(categoria.getTipo())) {
      throw new BusinessException(
          "A categoria informada não é compatível com o tipo do lançamento.");
    }
  }

  private Conta buscarCartaoDoUsuario(UUID contaId, UUID usuarioId) {
    Conta conta = contaService.buscarConta(contaId, usuarioId);

    if (!conta.ehCartaoDeCredito()) {
      throw new BusinessException("A conta informada não é um cartão de crédito.");
    }

    return conta;
  }

  private FaturaCartaoResponse montarFatura(
      Conta cartao, UUID usuarioId, Integer ano, Integer mes) {
    YearMonth mesDoVencimento = Competencia.de(ano, mes);
    LocalDate dataFechamento = cartao.fechamentoDaFatura(mesDoVencimento);
    List<LancamentoFinanceiro> lancamentos =
        lancamentoFinanceiroRepository.buscarDaFatura(
            usuarioId, cartao.getId(), mesDoVencimento.atDay(1), mesDoVencimento.atEndOfMonth());

    return new FaturaCartaoResponse(
        cartao.getId(),
        cartao.getNome(),
        ano,
        mes,
        dataFechamento,
        mesDoVencimento.atDay(cartao.getDiaVencimento()),
        hoje().isAfter(dataFechamento),
        somarFatura(lancamentos),
        lancamentos.stream().map(this::paraResponse).toList());
  }

  private static BigDecimal somarFatura(List<LancamentoFinanceiro> lancamentos) {
    return lancamentos.stream()
        .map(
            lancamento ->
                TipoLancamentoFinanceiro.DESPESA.equals(lancamento.getTipo())
                    ? lancamento.getValor()
                    : lancamento.getValor().negate())
        .reduce(Dinheiro.ZERO, BigDecimal::add);
  }

  private LancamentoFinanceiro buscarLancamentoDoUsuario(UUID id, UUID usuarioId) {
    return lancamentoFinanceiroRepository
        .buscarPorIdEUsuarioId(id, usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Lançamento financeiro não encontrado."));
  }

  private LancamentoFinanceiro travarLancamentoDoUsuario(UUID id, UUID usuarioId) {
    return lancamentoFinanceiroRepository
        .travarPorIdEUsuarioId(id, usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Lançamento financeiro não encontrado."));
  }
}
