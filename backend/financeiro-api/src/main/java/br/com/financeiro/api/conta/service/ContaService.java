package br.com.financeiro.api.conta.service;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.exception.ResourceNotFoundException;
import br.com.financeiro.api.conta.dto.ContaResponse;
import br.com.financeiro.api.conta.dto.CriarContaRequest;
import br.com.financeiro.api.conta.entity.Conta;
import br.com.financeiro.api.conta.mapper.ContaMapper;
import br.com.financeiro.api.conta.repository.ContaRepository;
import br.com.financeiro.api.usuario.entity.Usuario;
import br.com.financeiro.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
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

        String nomeNormalizado = normalizarNome(request.nome());

        validarDuplicidade(
                request.usuarioId(),
                nomeNormalizado
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

    private String normalizarNome(String nome) {
        return nome == null ? null : nome.trim();
    }
}