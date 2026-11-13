package br.com.financeiro.api.lancamentofinanceiro.service;

import br.com.financeiro.api.categoria.entity.Categoria;
import br.com.financeiro.api.categoria.repository.CategoriaRepository;
import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.exception.ResourceNotFoundException;
import br.com.financeiro.api.common.util.NormalizadorTexto;
import br.com.financeiro.api.conta.entity.Conta;
import br.com.financeiro.api.conta.repository.ContaRepository;
import br.com.financeiro.api.lancamentofinanceiro.dto.AtualizarLancamentoFinanceiroRequest;
import br.com.financeiro.api.lancamentofinanceiro.dto.CriarLancamentoFinanceiroRequest;
import br.com.financeiro.api.lancamentofinanceiro.dto.LancamentoFinanceiroResponse;
import br.com.financeiro.api.lancamentofinanceiro.dto.PagarLancamentoFinanceiroRequest;
import br.com.financeiro.api.lancamentofinanceiro.entity.LancamentoFinanceiro;
import br.com.financeiro.api.lancamentofinanceiro.enums.StatusLancamentoFinanceiro;
import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import br.com.financeiro.api.lancamentofinanceiro.mapper.LancamentoFinanceiroMapper;
import br.com.financeiro.api.lancamentofinanceiro.repository.LancamentoFinanceiroRepository;
import br.com.financeiro.api.usuario.entity.Usuario;
import br.com.financeiro.api.usuario.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LancamentoFinanceiroService {

  private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
  private final UsuarioRepository usuarioRepository;
  private final ContaRepository contaRepository;
  private final CategoriaRepository categoriaRepository;
  private final LancamentoFinanceiroMapper lancamentoFinanceiroMapper;
  private final Clock clock;

  @Transactional
  public LancamentoFinanceiroResponse criar(
      UUID usuarioId, CriarLancamentoFinanceiroRequest request) {
    Usuario usuario = buscarUsuario(usuarioId);
    Conta conta = buscarContaAtivaDoUsuario(request.contaId(), usuarioId);
    Categoria categoria = buscarCategoriaAtivaDoUsuario(request.categoriaId(), usuarioId);

    validarCategoriaCompativelComTipo(categoria, request.tipo());
    validarDataPagamento(request.dataPagamento());

    StatusLancamentoFinanceiro status =
        definirStatusInicial(request.dataPagamento(), request.dataVencimento());

    LancamentoFinanceiro lancamentoFinanceiro = lancamentoFinanceiroMapper.paraEntity(request);
    lancamentoFinanceiro.setUsuario(usuario);
    lancamentoFinanceiro.setConta(conta);
    lancamentoFinanceiro.setCategoria(categoria);
    lancamentoFinanceiro.setDescricao(NormalizadorTexto.normalizarNome(request.descricao()));
    lancamentoFinanceiro.setStatus(status);

    movimentarSaldoSePago(lancamentoFinanceiro);

    LancamentoFinanceiro lancamentoSalvo =
        lancamentoFinanceiroRepository.saveAndFlush(lancamentoFinanceiro);
    return lancamentoFinanceiroMapper.paraResponse(lancamentoSalvo);
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
    buscarUsuario(usuarioId);
    validarPeriodo(dataInicio, dataFim);
    validarContaDoUsuario(contaId, usuarioId);
    validarCategoriaDoUsuario(categoriaId, usuarioId);

    return lancamentoFinanceiroRepository
        .findAll(
            montarFiltros(usuarioId, tipo, status, contaId, categoriaId, dataInicio, dataFim),
            pageable)
        .map(lancamentoFinanceiroMapper::paraResponse);
  }

  @Transactional(readOnly = true)
  public LancamentoFinanceiroResponse buscarPorId(UUID id, UUID usuarioId) {
    LancamentoFinanceiro lancamentoFinanceiro = buscarLancamentoDoUsuario(id, usuarioId);
    return lancamentoFinanceiroMapper.paraResponse(lancamentoFinanceiro);
  }

  @Transactional
  public LancamentoFinanceiroResponse atualizar(
      UUID id, UUID usuarioId, AtualizarLancamentoFinanceiroRequest request) {
    LancamentoFinanceiro lancamentoFinanceiro = buscarLancamentoDoUsuario(id, usuarioId);

    if (StatusLancamentoFinanceiro.CANCELADO.equals(lancamentoFinanceiro.getStatus())) {
      throw new BusinessException("Não é possível editar um lançamento cancelado.");
    }

    Conta conta = buscarContaAtivaDoUsuario(request.contaId(), usuarioId);
    Categoria categoria = buscarCategoriaAtivaDoUsuario(request.categoriaId(), usuarioId);

    validarCategoriaCompativelComTipo(categoria, request.tipo());
    validarDataPagamento(request.dataPagamento());

    estornarSaldoSePago(lancamentoFinanceiro);

    StatusLancamentoFinanceiro status =
        definirStatusInicial(request.dataPagamento(), request.dataVencimento());

    lancamentoFinanceiro.setConta(conta);
    lancamentoFinanceiro.setCategoria(categoria);
    lancamentoFinanceiro.setDescricao(NormalizadorTexto.normalizarNome(request.descricao()));
    lancamentoFinanceiro.setTipo(request.tipo());
    lancamentoFinanceiro.setValor(request.valor());
    lancamentoFinanceiro.setDataCompetencia(request.dataCompetencia());
    lancamentoFinanceiro.setDataVencimento(request.dataVencimento());
    lancamentoFinanceiro.setDataPagamento(request.dataPagamento());
    lancamentoFinanceiro.setStatus(status);
    lancamentoFinanceiro.setObservacao(request.observacao());

    movimentarSaldoSePago(lancamentoFinanceiro);
    lancamentoFinanceiroRepository.flush();

    return lancamentoFinanceiroMapper.paraResponse(lancamentoFinanceiro);
  }

  @Transactional
  public LancamentoFinanceiroResponse pagar(
      UUID id, UUID usuarioId, PagarLancamentoFinanceiroRequest request) {
    LancamentoFinanceiro lancamentoFinanceiro = buscarLancamentoDoUsuario(id, usuarioId);

    if (StatusLancamentoFinanceiro.CANCELADO.equals(lancamentoFinanceiro.getStatus())) {
      throw new BusinessException("Não é possível pagar um lançamento cancelado.");
    }

    if (StatusLancamentoFinanceiro.PAGO.equals(lancamentoFinanceiro.getStatus())) {
      throw new BusinessException("O lançamento informado já está pago.");
    }

    validarDataPagamento(request.dataPagamento());

    lancamentoFinanceiro.setDataPagamento(request.dataPagamento());
    lancamentoFinanceiro.setStatus(StatusLancamentoFinanceiro.PAGO);
    movimentarSaldo(
        lancamentoFinanceiro.getConta(),
        lancamentoFinanceiro.getTipo(),
        lancamentoFinanceiro.getValor());
    lancamentoFinanceiroRepository.flush();

    return lancamentoFinanceiroMapper.paraResponse(lancamentoFinanceiro);
  }

  @Transactional
  public void cancelar(UUID id, UUID usuarioId) {
    LancamentoFinanceiro lancamentoFinanceiro = buscarLancamentoDoUsuario(id, usuarioId);

    if (StatusLancamentoFinanceiro.CANCELADO.equals(lancamentoFinanceiro.getStatus())) {
      throw new BusinessException("O lançamento informado já está cancelado.");
    }

    estornarSaldoSePago(lancamentoFinanceiro);
    lancamentoFinanceiro.setDataPagamento(null);
    lancamentoFinanceiro.setStatus(StatusLancamentoFinanceiro.CANCELADO);
  }

  private void movimentarSaldoSePago(LancamentoFinanceiro lancamentoFinanceiro) {
    if (StatusLancamentoFinanceiro.PAGO.equals(lancamentoFinanceiro.getStatus())) {
      movimentarSaldo(
          lancamentoFinanceiro.getConta(),
          lancamentoFinanceiro.getTipo(),
          lancamentoFinanceiro.getValor());
    }
  }

  private void estornarSaldoSePago(LancamentoFinanceiro lancamentoFinanceiro) {
    if (StatusLancamentoFinanceiro.PAGO.equals(lancamentoFinanceiro.getStatus())) {
      estornarSaldo(
          lancamentoFinanceiro.getConta(),
          lancamentoFinanceiro.getTipo(),
          lancamentoFinanceiro.getValor());
    }
  }

  private void movimentarSaldo(Conta conta, TipoLancamentoFinanceiro tipo, BigDecimal valor) {
    BigDecimal saldoAtual = conta.getSaldoAtual();

    if (TipoLancamentoFinanceiro.RECEITA.equals(tipo)) {
      conta.setSaldoAtual(saldoAtual.add(valor));
      return;
    }

    conta.setSaldoAtual(saldoAtual.subtract(valor));
  }

  private void estornarSaldo(Conta conta, TipoLancamentoFinanceiro tipo, BigDecimal valor) {
    BigDecimal saldoAtual = conta.getSaldoAtual();

    if (TipoLancamentoFinanceiro.RECEITA.equals(tipo)) {
      conta.setSaldoAtual(saldoAtual.subtract(valor));
      return;
    }

    conta.setSaldoAtual(saldoAtual.add(valor));
  }

  private StatusLancamentoFinanceiro definirStatusInicial(
      LocalDate dataPagamento, LocalDate dataVencimento) {
    if (dataPagamento != null) {
      return StatusLancamentoFinanceiro.PAGO;
    }

    if (dataVencimento != null && dataVencimento.isBefore(LocalDate.now(clock))) {
      return StatusLancamentoFinanceiro.ATRASADO;
    }

    return StatusLancamentoFinanceiro.PENDENTE;
  }

  private void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
    if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
      throw new BusinessException("A data inicial não pode ser posterior a data final.");
    }
  }

  private Specification<LancamentoFinanceiro> montarFiltros(
      UUID usuarioId,
      TipoLancamentoFinanceiro tipo,
      StatusLancamentoFinanceiro status,
      UUID contaId,
      UUID categoriaId,
      LocalDate dataInicio,
      LocalDate dataFim) {
    return (root, query, criteriaBuilder) -> {
      var predicate = criteriaBuilder.equal(root.get("usuario").get("id"), usuarioId);

      if (tipo != null) {
        predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("tipo"), tipo));
      }

      if (status != null) {
        predicate =
            criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
      }

      if (contaId != null) {
        predicate =
            criteriaBuilder.and(
                predicate, criteriaBuilder.equal(root.get("conta").get("id"), contaId));
      }

      if (categoriaId != null) {
        predicate =
            criteriaBuilder.and(
                predicate, criteriaBuilder.equal(root.get("categoria").get("id"), categoriaId));
      }

      if (dataInicio != null) {
        predicate =
            criteriaBuilder.and(
                predicate,
                criteriaBuilder.greaterThanOrEqualTo(root.get("dataCompetencia"), dataInicio));
      }

      if (dataFim != null) {
        predicate =
            criteriaBuilder.and(
                predicate, criteriaBuilder.lessThanOrEqualTo(root.get("dataCompetencia"), dataFim));
      }

      return predicate;
    };
  }

  private void validarDataPagamento(LocalDate dataPagamento) {
    if (dataPagamento != null && dataPagamento.isAfter(LocalDate.now(clock))) {
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

  private Usuario buscarUsuario(UUID usuarioId) {
    return usuarioRepository
        .findById(usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
  }

  private LancamentoFinanceiro buscarLancamentoDoUsuario(UUID id, UUID usuarioId) {
    return lancamentoFinanceiroRepository
        .buscarPorIdEUsuarioId(id, usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Lançamento financeiro não encontrado."));
  }

  private Conta buscarContaAtivaDoUsuario(UUID contaId, UUID usuarioId) {
    Conta conta =
        contaRepository
            .buscarPorIdEUsuarioId(contaId, usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada."));

    if (!Boolean.TRUE.equals(conta.getAtiva())) {
      throw new BusinessException("A conta informada está inativa.");
    }

    return conta;
  }

  private Categoria buscarCategoriaAtivaDoUsuario(UUID categoriaId, UUID usuarioId) {
    Categoria categoria =
        categoriaRepository
            .buscarPorIdEUsuarioId(categoriaId, usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));

    if (!Boolean.TRUE.equals(categoria.getAtiva())) {
      throw new BusinessException("A categoria informada está inativa.");
    }

    return categoria;
  }

  private void validarContaDoUsuario(UUID contaId, UUID usuarioId) {
    if (contaId == null) {
      return;
    }

    contaRepository
        .buscarPorIdEUsuarioId(contaId, usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada."));
  }

  private void validarCategoriaDoUsuario(UUID categoriaId, UUID usuarioId) {
    if (categoriaId == null) {
      return;
    }

    categoriaRepository
        .buscarPorIdEUsuarioId(categoriaId, usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));
  }
}
