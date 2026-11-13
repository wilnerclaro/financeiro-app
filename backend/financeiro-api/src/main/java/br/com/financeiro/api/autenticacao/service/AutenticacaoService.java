package br.com.financeiro.api.autenticacao.service;

import br.com.financeiro.api.autenticacao.dto.CadastroRequest;
import br.com.financeiro.api.autenticacao.dto.LoginRequest;
import br.com.financeiro.api.autenticacao.dto.RefreshTokenRequest;
import br.com.financeiro.api.autenticacao.dto.TokenResponse;
import br.com.financeiro.api.autenticacao.dto.UsuarioResponse;
import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.exception.NaoAutenticadoException;
import br.com.financeiro.api.common.exception.ResourceNotFoundException;
import br.com.financeiro.api.common.util.NormalizadorTexto;
import br.com.financeiro.api.security.TokenJwt;
import br.com.financeiro.api.usuario.entity.Usuario;
import br.com.financeiro.api.usuario.repository.UsuarioRepository;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AutenticacaoService {

  private static final String CREDENCIAIS_INVALIDAS = "E-mail ou senha inválidos.";

  private final UsuarioRepository usuarioRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenJwt tokenJwt;

  @Transactional
  public TokenResponse cadastrar(CadastroRequest request) {
    String email = normalizarEmail(request.email());

    if (usuarioRepository.existsByEmailIgnoreCase(email)) {
      throw new BusinessException("Já existe um usuário cadastrado com esse e-mail.");
    }

    Usuario usuario = new Usuario();
    usuario.setNome(NormalizadorTexto.normalizarNome(request.nome()));
    usuario.setEmail(email);
    usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
    usuario.setAtivo(true);

    return emitirTokens(usuarioRepository.saveAndFlush(usuario));
  }

  @Transactional(readOnly = true)
  public TokenResponse login(LoginRequest request) {
    Usuario usuario =
        usuarioRepository
            .findByEmailIgnoreCase(normalizarEmail(request.email()))
            .filter(
                encontrado -> passwordEncoder.matches(request.senha(), encontrado.getSenhaHash()))
            .orElseThrow(() -> new NaoAutenticadoException(CREDENCIAIS_INVALIDAS));

    validarAtivo(usuario);
    return emitirTokens(usuario);
  }

  @Transactional(readOnly = true)
  public TokenResponse renovar(RefreshTokenRequest request) {
    UUID usuarioId;

    try {
      usuarioId = tokenJwt.validarTokenDeRefresh(request.refreshToken());
    } catch (JwtException exception) {
      throw new NaoAutenticadoException("Refresh token inválido ou expirado.");
    }

    Usuario usuario =
        usuarioRepository
            .findById(usuarioId)
            .orElseThrow(() -> new NaoAutenticadoException("Refresh token inválido ou expirado."));

    validarAtivo(usuario);
    return emitirTokens(usuario);
  }

  @Transactional(readOnly = true)
  public UsuarioResponse buscarUsuarioLogado(UUID usuarioId) {
    return usuarioRepository
        .findById(usuarioId)
        .map(this::paraResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
  }

  private TokenResponse emitirTokens(Usuario usuario) {
    return new TokenResponse(
        tokenJwt.gerarTokenDeAcesso(usuario.getId(), usuario.getEmail()),
        tokenJwt.gerarTokenDeRefresh(usuario.getId(), usuario.getEmail()),
        "Bearer",
        tokenJwt.duracaoAcessoEmSegundos(),
        paraResponse(usuario));
  }

  private void validarAtivo(Usuario usuario) {
    if (!Boolean.TRUE.equals(usuario.getAtivo())) {
      throw new NaoAutenticadoException("Usuário inativo.");
    }
  }

  private UsuarioResponse paraResponse(Usuario usuario) {
    return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
  }

  private static String normalizarEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
