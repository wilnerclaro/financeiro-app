package br.com.financeiro.api.conta.service;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.exception.ResourceNotFoundException;
import br.com.financeiro.api.common.util.NormalizadorTexto;
import br.com.financeiro.api.conta.dto.AtualizarContaRequest;
import br.com.financeiro.api.conta.dto.ContaResponse;
import br.com.financeiro.api.conta.dto.CorrigirSaldoInicialContaRequest;
import br.com.financeiro.api.conta.dto.CriarContaRequest;
import br.com.financeiro.api.conta.entity.Conta;
import br.com.financeiro.api.conta.enums.TipoConta;
import br.com.financeiro.api.conta.mapper.ContaMapper;
import br.com.financeiro.api.conta.repository.ContaRepository;
import br.com.financeiro.api.lancamentofinanceiro.repository.LancamentoFinanceiroRepository;
import br.com.financeiro.api.usuario.entity.Usuario;
import br.com.financeiro.api.usuario.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContaService {

  private final ContaRepository contaRepository;
  private final UsuarioRepository usuarioRepository;
  private final ContaMapper contaMapper;
  private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;

  @Transactional
  public ContaResponse criar(CriarContaRequest request) {
    Usuario usuario = buscarUsuario(request.usuarioId());
    String nomeNormalizado = NormalizadorTexto.normalizarNome(request.nome());

    validarDuplicidade(request.usuarioId(), nomeNormalizado, null);

    Conta conta = contaMapper.paraEntity(request);
    conta.setUsuario(usuario);
    conta.setNome(nomeNormalizado.toUpperCase());
    conta.setSaldoAtual(request.saldoInicial());
    conta.setAtiva(true);

    Conta contaSalva = contaRepository.save(conta);
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
  public ContaResponse atualizar(UUID id, AtualizarContaRequest request) {
    Conta conta = buscarContaDoUsuario(id, request.usuarioId());
    String nomeNormalizado = NormalizadorTexto.normalizarNome(request.nome());

    validarDuplicidade(request.usuarioId(), nomeNormalizado, id);

    conta.setNome(nomeNormalizado.toUpperCase());
    conta.setTipo(request.tipo());
    conta.setAtiva(request.ativa());

    return contaMapper.paraResponse(conta);
  }

  @Transactional
  public ContaResponse corrigirSaldoInicial(UUID id, CorrigirSaldoInicialContaRequest request) {
    Conta conta = buscarContaDoUsuario(id, request.usuarioId());
    BigDecimal saldoMovimentado =
        lancamentoFinanceiroRepository.calcularSaldoMovimentadoPorConta(id, request.usuarioId());

    conta.setSaldoInicial(request.saldoInicial());
    conta.setSaldoAtual(request.saldoInicial().add(saldoMovimentado));

    return contaMapper.paraResponse(conta);
  }

  @Transactional
  public void inativar(UUID id, UUID usuarioId) {
    Conta conta = buscarContaDoUsuario(id, usuarioId);

    if (!Boolean.TRUE.equals(conta.getAtiva())) {
      throw new BusinessException("A conta informada ja esta inativa.");
    }

    conta.setAtiva(false);
  }

  @Transactional
  public ContaResponse ativar(UUID id, UUID usuarioId) {
    Conta conta = buscarContaDoUsuario(id, usuarioId);

    if (Boolean.TRUE.equals(conta.getAtiva())) {
      throw new BusinessException("A conta informada ja esta ativa.");
    }

    conta.setAtiva(true);

    return contaMapper.paraResponse(conta);
  }

  private Usuario buscarUsuario(UUID usuarioId) {
    return usuarioRepository
        .findById(usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));
  }

  private Conta buscarContaDoUsuario(UUID contaId, UUID usuarioId) {
    return contaRepository
        .buscarPorIdEUsuarioId(contaId, usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Conta nao encontrada."));
  }

  private void validarDuplicidade(UUID usuarioId, String nome, UUID contaIdIgnorada) {
    boolean duplicada = contaRepository.existeContaDuplicada(usuarioId, nome, contaIdIgnorada);

    if (duplicada) {
      throw new BusinessException("Ja existe uma conta com esse nome para este usuario.");
    }
  }
}
