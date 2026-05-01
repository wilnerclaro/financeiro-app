package br.com.financeiro.api.conta.service;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.exception.ResourceNotFoundException;
import br.com.financeiro.api.common.util.NormalizadorTexto;
import br.com.financeiro.api.conta.dto.ContaResponse;
import br.com.financeiro.api.conta.dto.CriarContaRequest;
import br.com.financeiro.api.conta.entity.Conta;
import br.com.financeiro.api.conta.enums.TipoConta;
import br.com.financeiro.api.conta.mapper.ContaMapper;
import br.com.financeiro.api.conta.repository.ContaRepository;
import br.com.financeiro.api.usuario.entity.Usuario;
import br.com.financeiro.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContaService {

    private final ContaRepository contaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ContaMapper contaMapper;

    @Transactional
    public ContaResponse criar(CriarContaRequest request) {
        Usuario usuario = buscarUsuario(request.usuarioId());

        String nomeNormalizado = NormalizadorTexto.normalizarNome(request.nome());
        String nomeParaComparacao = NormalizadorTexto.normalizarParaComparacao(request.nome());

        validarDuplicidade(
                request.usuarioId(),
                nomeParaComparacao
        );

        Conta conta = contaMapper.paraEntity(request);
        conta.setUsuario(usuario);
        conta.setNome(nomeNormalizado.toUpperCase());
        conta.setSaldoAtual(request.saldoInicial());
        conta.setAtiva(true);

        Conta contaSalva = contaRepository.save(conta);

        return contaMapper.paraResponse(contaSalva);
    }

    private Usuario buscarUsuario(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    private void validarDuplicidade(UUID usuarioId, String nome) {
        boolean duplicada = contaRepository.existeContaDuplicada(usuarioId, nome);

        if (duplicada) {
            throw new BusinessException("Já existe uma conta com esse nome para este usuário.");
        }
    }

    @Transactional(readOnly = true)
    public Page<ContaResponse> listar(UUID usuarioId,
                                      TipoConta tipoConta,
                                      Boolean ativa,
                                      Pageable pageable) {
        buscarUsuario(usuarioId);
        return contaRepository.buscarPorFiltros(usuarioId, tipoConta, ativa, pageable)
                .map(contaMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public ContaResponse buscarPorId(UUID id, UUID usuarioId) {
        Conta conta = buscarContaDoUsuaurio(id, usuarioId);
        return contaMapper.paraResponse(conta);
    }

    private Conta buscarContaDoUsuaurio(UUID contaId, UUID usuarioId) {
        return contaRepository.buscarPorIdEUsuarioId(contaId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada."));
    }
}