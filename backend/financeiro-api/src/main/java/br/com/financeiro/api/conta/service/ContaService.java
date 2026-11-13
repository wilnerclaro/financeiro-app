package br.com.financeiro.api.conta.service;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.exception.ResourceNotFoundException;
import br.com.financeiro.api.common.util.NormalizadorTexto;
import br.com.financeiro.api.conta.dto.AtualizarContaRequest;
import br.com.financeiro.api.conta.dto.ContaResponse;
import br.com.financeiro.api.conta.dto.CorrigirSaldoInicialContaRequest;
import br.com.financeiro.api.conta.dto.CriarContaRequest;
import br.com.financeiro.api.conta.dto.ResumoContasResponse;
import br.com.financeiro.api.conta.entity.Conta;
import br.com.financeiro.api.conta.enums.TipoConta;
import br.com.financeiro.api.conta.mapper.ContaMapper;
import br.com.financeiro.api.conta.repository.ContaRepository;
import br.com.financeiro.api.usuario.entity.Usuario;
import br.com.financeiro.api.usuario.service.UsuarioService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContaService {

  private final ContaRepository contaRepository;
  private final UsuarioService usuarioService;
  private final ContaMapper contaMapper;
  private final EntityManager entityManager;

  @Transactional
  public ContaResponse criar(UUID usuarioId, CriarContaRequest request) {
    Usuario usuario = buscarUsuario(usuarioId);
    String nomeNormalizado = NormalizadorTexto.normalizarNome(request.nome());

    validarDuplicidade(usuarioId, nomeNormalizado, null);
    validarDiasDaFatura(request.tipo(), request.diaFechamento(), request.diaVencimento());

    Conta conta = contaMapper.paraEntity(request);
    conta.setUsuario(usuario);
    conta.setNome(nomeNormalizado);
    conta.setSaldoAtual(request.saldoInicial());
    conta.setAtiva(true);
    definirDiasDaFatura(conta, request.tipo(), request.diaFechamento(), request.diaVencimento());

    Conta contaSalva = contaRepository.saveAndFlush(conta);
    return contaMapper.paraResponse(contaSalva);
  }

  @Transactional(readOnly = true)
  public Page<ContaResponse> listar(
      UUID usuarioId, TipoConta tipoConta, Boolean ativa, Pageable pageable) {
    buscarUsuario(usuarioId);
    return contaRepository
        .buscarPorFiltros(usuarioId, tipoConta, ativa, pageable)
        .map(contaMapper::paraResponse);
  }

  @Transactional(readOnly = true)
  public ContaResponse buscarPorId(UUID id, UUID usuarioId) {
    Conta conta = buscarContaDoUsuario(id, usuarioId);
    return contaMapper.paraResponse(conta);
  }

  @Transactional
  public ContaResponse atualizar(UUID id, UUID usuarioId, AtualizarContaRequest request) {
    Conta conta = buscarContaDoUsuario(id, usuarioId);
    String nomeNormalizado = NormalizadorTexto.normalizarNome(request.nome());

    validarDuplicidade(usuarioId, nomeNormalizado, id);
    validarDiasDaFatura(request.tipo(), request.diaFechamento(), request.diaVencimento());

    conta.setNome(nomeNormalizado);
    conta.setTipo(request.tipo());
    definirDiasDaFatura(conta, request.tipo(), request.diaFechamento(), request.diaVencimento());
    contaRepository.flush();

    return contaMapper.paraResponse(conta);
  }

  @Transactional
  public ContaResponse corrigirSaldoInicial(
      UUID id, UUID usuarioId, CorrigirSaldoInicialContaRequest request) {
    Conta conta = buscarContaDoUsuario(id, usuarioId);
    BigDecimal saldoMovimentado = contaRepository.calcularSaldoMovimentado(id, usuarioId);

    conta.setSaldoInicial(request.saldoInicial());
    conta.setSaldoAtual(request.saldoInicial().add(saldoMovimentado));
    contaRepository.flush();

    return contaMapper.paraResponse(conta);
  }

  @Transactional
  public void inativar(UUID id, UUID usuarioId) {
    Conta conta = buscarContaDoUsuario(id, usuarioId);
    conta.setAtiva(false);
  }

  @Transactional
  public ContaResponse ativar(UUID id, UUID usuarioId) {
    Conta conta = buscarContaDoUsuario(id, usuarioId);
    conta.setAtiva(true);
    contaRepository.flush();

    return contaMapper.paraResponse(conta);
  }

  private void validarDiasDaFatura(TipoConta tipo, Integer diaFechamento, Integer diaVencimento) {
    if (TipoConta.CARTAO_CREDITO.equals(tipo) && (diaFechamento == null || diaVencimento == null)) {
      throw new BusinessException(
          "Cartão de crédito exige o dia de fechamento e o dia de vencimento da fatura.");
    }
  }

  private void definirDiasDaFatura(
      Conta conta, TipoConta tipo, Integer diaFechamento, Integer diaVencimento) {
    boolean cartao = TipoConta.CARTAO_CREDITO.equals(tipo);
    conta.setDiaFechamento(cartao ? diaFechamento : null);
    conta.setDiaVencimento(cartao ? diaVencimento : null);
  }

  private Usuario buscarUsuario(UUID usuarioId) {
    return usuarioService.buscar(usuarioId);
  }

  @Transactional(readOnly = true)
  public Conta buscarConta(UUID contaId, UUID usuarioId) {
    return buscarContaDoUsuario(contaId, usuarioId);
  }

  @Transactional(readOnly = true)
  public Conta buscarContaAtiva(UUID contaId, UUID usuarioId) {
    Conta conta = buscarContaDoUsuario(contaId, usuarioId);

    if (!Boolean.TRUE.equals(conta.getAtiva())) {
      throw new BusinessException("A conta informada está inativa.");
    }

    return conta;
  }

  @Transactional(readOnly = true)
  public void validarContaDoUsuario(UUID contaId, UUID usuarioId) {
    if (contaId != null) {
      buscarContaDoUsuario(contaId, usuarioId);
    }
  }

  @Transactional(readOnly = true)
  public ResumoContasResponse resumir(UUID usuarioId) {
    List<ResumoContasResponse.TotalPorTipo> porTipo =
        contaRepository.somarPorTipo(usuarioId).stream()
            .map(
                t ->
                    new ResumoContasResponse.TotalPorTipo(
                        t.getTipo(), t.getTotal().setScale(2), t.getQuantidade()))
            .toList();
    BigDecimal patrimonio =
        porTipo.stream()
            .map(ResumoContasResponse.TotalPorTipo::total)
            .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    return new ResumoContasResponse(patrimonio, porTipo);
  }

  @Transactional
  public void creditar(Conta conta, BigDecimal valor) {
    travar(conta);
    conta.creditar(valor);
  }

  @Transactional
  public void debitar(Conta conta, BigDecimal valor) {
    travar(conta);
    conta.debitar(valor);
  }

  @Transactional
  public void travar(Conta... contas) {
    Stream.of(contas)
        .distinct()
        .sorted(Comparator.comparing(Conta::getId))
        .filter(conta -> entityManager.getLockMode(conta) != LockModeType.PESSIMISTIC_WRITE)
        .forEach(conta -> entityManager.refresh(conta, LockModeType.PESSIMISTIC_WRITE));
  }

  @Transactional(readOnly = true)
  public BigDecimal somarSaldoEmContas(UUID usuarioId) {
    return contaRepository.somarSaldoEmContas(usuarioId);
  }

  private Conta buscarContaDoUsuario(UUID contaId, UUID usuarioId) {
    return contaRepository
        .buscarPorIdEUsuarioId(contaId, usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada."));
  }

  private void validarDuplicidade(UUID usuarioId, String nome, UUID contaIdIgnorada) {
    boolean duplicada = contaRepository.existeContaDuplicada(usuarioId, nome, contaIdIgnorada);

    if (duplicada) {
      throw new BusinessException("Já existe uma conta com esse nome para este usuário.");
    }
  }
}
