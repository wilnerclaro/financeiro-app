package br.com.financeiro.api.autenticacao.service;

import br.com.financeiro.api.autenticacao.dto.CadastroRequest;
import br.com.financeiro.api.autenticacao.dto.ConfiguracaoAutenticacaoResponse;
import br.com.financeiro.api.autenticacao.dto.EntrarComGoogleRequest;
import br.com.financeiro.api.autenticacao.dto.LoginRequest;
import br.com.financeiro.api.autenticacao.dto.RefreshTokenRequest;
import br.com.financeiro.api.autenticacao.dto.TokenResponse;
import br.com.financeiro.api.autenticacao.dto.UsuarioResponse;
import br.com.financeiro.api.categoria.service.CategoriasPadraoService;
import br.com.financeiro.api.common.exception.ConflitoException;
import br.com.financeiro.api.common.exception.NaoAutenticadoException;
import br.com.financeiro.api.common.util.NormalizadorTexto;
import br.com.financeiro.api.security.GooglePropriedades;
import br.com.financeiro.api.security.IdentidadeGoogle;
import br.com.financeiro.api.security.TokenJwt;
import br.com.financeiro.api.security.VerificadorTokenGoogle;
import br.com.financeiro.api.usuario.entity.Usuario;
import br.com.financeiro.api.usuario.enums.ProvedorIdentidade;
import br.com.financeiro.api.usuario.service.UsuarioService;
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
  private static final int TAMANHO_MAXIMO_NOME = 120;

  private final UsuarioService usuarioService;
  private final CategoriasPadraoService categoriasPadraoService;
  private final PasswordEncoder passwordEncoder;
  private final TokenJwt tokenJwt;
  private final VerificadorTokenGoogle verificadorTokenGoogle;
  private final GooglePropriedades googlePropriedades;

  @Transactional
  public TokenResponse cadastrar(CadastroRequest request) {
    String email = normalizarEmail(request.email());

    if (usuarioService.existeComEmail(email)) {
      throw new ConflitoException("Já existe uma conta com este e-mail.");
    }

    Usuario salvo = criarUsuario(request.nome(), email, passwordEncoder.encode(request.senha()));
    return emitirTokens(salvo);
  }

  @Transactional
  public TokenResponse entrarComGoogle(EntrarComGoogleRequest request) {
    IdentidadeGoogle identidade = verificadorTokenGoogle.verificar(request.idToken());

    Usuario usuario =
        usuarioService
            .buscarPorIdentidade(ProvedorIdentidade.GOOGLE, identidade.sujeito())
            .orElseGet(() -> ligarOuCriarUsuarioDoGoogle(identidade));

    validarAtivo(usuario);
    return emitirTokens(usuario);
  }

  @Transactional(readOnly = true)
  public ConfiguracaoAutenticacaoResponse buscarConfiguracao() {
    return new ConfiguracaoAutenticacaoResponse(
        googlePropriedades.configurado() ? googlePropriedades.clientId() : null);
  }

  @Transactional(readOnly = true)
  public TokenResponse login(LoginRequest request) {
    Usuario usuario =
        usuarioService
            .buscarPorEmail(normalizarEmail(request.email()))
            .filter(
                encontrado ->
                    encontrado.getSenhaHash() != null
                        && passwordEncoder.matches(request.senha(), encontrado.getSenhaHash()))
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
        usuarioService
            .buscarSeExistir(usuarioId)
            .orElseThrow(() -> new NaoAutenticadoException("Refresh token inválido ou expirado."));

    validarAtivo(usuario);
    return emitirTokens(usuario);
  }

  @Transactional(readOnly = true)
  public UsuarioResponse buscarUsuarioLogado(UUID usuarioId) {
    return paraResponse(usuarioService.buscar(usuarioId));
  }

  private Usuario ligarOuCriarUsuarioDoGoogle(IdentidadeGoogle identidade) {
    String email = normalizarEmail(identidade.email());
    Usuario usuario =
        usuarioService
            .buscarPorEmail(email)
            .orElseGet(() -> criarUsuario(nomeDoGoogle(identidade, email), email, null));
    usuarioService.ligarIdentidade(usuario, ProvedorIdentidade.GOOGLE, identidade.sujeito());
    return usuario;
  }

  private Usuario criarUsuario(String nome, String email, String senhaHash) {
    Usuario usuario = new Usuario();
    usuario.setNome(NormalizadorTexto.normalizarNome(nome));
    usuario.setEmail(email);
    usuario.setSenhaHash(senhaHash);
    usuario.setAtivo(true);

    Usuario salvo = usuarioService.salvar(usuario);
    categoriasPadraoService.criarPara(salvo.getId());
    return salvo;
  }

  private static String nomeDoGoogle(IdentidadeGoogle identidade, String email) {
    String nome =
        identidade.nome() == null || identidade.nome().isBlank()
            ? email.substring(0, email.indexOf('@'))
            : identidade.nome();
    return nome.length() > TAMANHO_MAXIMO_NOME ? nome.substring(0, TAMANHO_MAXIMO_NOME) : nome;
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
