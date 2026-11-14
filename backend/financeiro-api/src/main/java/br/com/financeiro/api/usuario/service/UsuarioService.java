package br.com.financeiro.api.usuario.service;

import br.com.financeiro.api.common.exception.ResourceNotFoundException;
import br.com.financeiro.api.usuario.entity.Usuario;
import br.com.financeiro.api.usuario.repository.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

  private final UsuarioRepository usuarioRepository;

  @Transactional(readOnly = true)
  public Usuario buscar(UUID usuarioId) {
    return usuarioRepository
        .findById(usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
  }

  @Transactional(readOnly = true)
  public Optional<Usuario> buscarSeExistir(UUID usuarioId) {
    return usuarioRepository.findById(usuarioId);
  }

  @Transactional(readOnly = true)
  public Optional<Usuario> buscarPorEmail(String email) {
    return usuarioRepository.findByEmailIgnoreCase(email);
  }

  @Transactional(readOnly = true)
  public boolean existeComEmail(String email) {
    return usuarioRepository.existsByEmailIgnoreCase(email);
  }

  @Transactional(readOnly = true)
  public void validarExistencia(UUID usuarioId) {
    if (!usuarioRepository.existsById(usuarioId)) {
      throw new ResourceNotFoundException("Usuário não encontrado.");
    }
  }

  @Transactional(readOnly = true)
  public Usuario referencia(UUID usuarioId) {
    return usuarioRepository.getReferenceById(usuarioId);
  }

  @Transactional
  public Usuario salvar(Usuario usuario) {
    return usuarioRepository.saveAndFlush(usuario);
  }
}
