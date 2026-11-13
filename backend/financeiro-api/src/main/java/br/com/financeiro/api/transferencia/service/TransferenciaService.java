package br.com.financeiro.api.transferencia.service;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.exception.ResourceNotFoundException;
import br.com.financeiro.api.common.util.NormalizadorTexto;
import br.com.financeiro.api.conta.entity.Conta;
import br.com.financeiro.api.conta.service.ContaService;
import br.com.financeiro.api.transferencia.dto.CriarTransferenciaRequest;
import br.com.financeiro.api.transferencia.dto.TransferenciaResponse;
import br.com.financeiro.api.transferencia.entity.Transferencia;
import br.com.financeiro.api.transferencia.mapper.TransferenciaMapper;
import br.com.financeiro.api.transferencia.repository.TransferenciaRepository;
import br.com.financeiro.api.usuario.service.UsuarioService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferenciaService {

  private final TransferenciaRepository transferenciaRepository;
  private final ContaService contaService;
  private final UsuarioService usuarioService;
  private final TransferenciaMapper transferenciaMapper;
  private final Clock clock;

  @Transactional
  public TransferenciaResponse criar(UUID usuarioId, CriarTransferenciaRequest request) {
    if (request.contaOrigemId().equals(request.contaDestinoId())) {
      throw new BusinessException("A conta de origem e a de destino devem ser diferentes.");
    }

    if (request.dataTransferencia().isAfter(LocalDate.now(clock))) {
      throw new BusinessException("A data da transferência não pode ser futura.");
    }

    Conta origem = buscarContaAtiva(request.contaOrigemId(), usuarioId, "origem");
    Conta destino = buscarContaAtiva(request.contaDestinoId(), usuarioId, "destino");

    contaService.travar(origem, destino);
    contaService.debitar(origem, request.valor());
    contaService.creditar(destino, request.valor());

    Transferencia transferencia = new Transferencia();
    transferencia.setUsuario(usuarioService.referencia(usuarioId));
    transferencia.setContaOrigem(origem);
    transferencia.setContaDestino(destino);
    transferencia.setValor(request.valor());
    transferencia.setDataTransferencia(request.dataTransferencia());
    transferencia.setDescricao(NormalizadorTexto.normalizarNome(request.descricao()));
    transferencia.setCancelada(false);

    return transferenciaMapper.paraResponse(transferenciaRepository.saveAndFlush(transferencia));
  }

  @Transactional(readOnly = true)
  public Page<TransferenciaResponse> listar(
      UUID usuarioId, UUID contaId, LocalDate dataInicio, LocalDate dataFim, Pageable pageable) {
    if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
      throw new BusinessException("A data inicial não pode ser posterior a data final.");
    }

    return transferenciaRepository
        .buscarPorFiltros(usuarioId, contaId, dataInicio, dataFim, pageable)
        .map(transferenciaMapper::paraResponse);
  }

  @Transactional(readOnly = true)
  public TransferenciaResponse buscarPorId(UUID id, UUID usuarioId) {
    return transferenciaMapper.paraResponse(buscarDoUsuario(id, usuarioId));
  }

  @Transactional
  public void cancelar(UUID id, UUID usuarioId) {
    Transferencia transferencia = buscarDoUsuario(id, usuarioId);

    if (Boolean.TRUE.equals(transferencia.getCancelada())) {
      throw new BusinessException("A transferência informada já está cancelada.");
    }

    contaService.travar(transferencia.getContaOrigem(), transferencia.getContaDestino());
    contaService.creditar(transferencia.getContaOrigem(), transferencia.getValor());
    contaService.debitar(transferencia.getContaDestino(), transferencia.getValor());
    transferencia.setCancelada(true);
  }

  private Conta buscarContaAtiva(UUID contaId, UUID usuarioId, String papel) {
    Conta conta;
    try {
      conta = contaService.buscarConta(contaId, usuarioId);
    } catch (ResourceNotFoundException exception) {
      throw new ResourceNotFoundException("Conta de %s não encontrada.".formatted(papel));
    }

    if (!Boolean.TRUE.equals(conta.getAtiva())) {
      throw new BusinessException("A conta de %s está inativa.".formatted(papel));
    }

    return conta;
  }

  private Transferencia buscarDoUsuario(UUID id, UUID usuarioId) {
    return transferenciaRepository
        .buscarPorIdEUsuarioId(id, usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Transferência não encontrada."));
  }
}
