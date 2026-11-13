package br.com.financeiro.api.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/**
 * Emite e valida os tokens da API (HMAC-SHA256 com o segredo do {@code .env}).
 *
 * <p>São dois tipos, diferenciados pela claim {@code tipo}: o de <b>acesso</b> (curto, vai no
 * header {@code Authorization} de toda requisição) e o de <b>refresh</b> (longo, só serve para
 * pedir um novo par em {@code /api/auth/refresh}). Um não é aceito no lugar do outro.
 */
@Component
public class TokenJwt {

  public static final String CLAIM_TIPO = "tipo";
  public static final String TIPO_ACESSO = "acesso";
  public static final String TIPO_REFRESH = "refresh";
  public static final String CLAIM_EMAIL = "email";

  private final JwtPropriedades propriedades;
  private final Clock clock;
  private final JwtEncoder codificador;
  private final JwtDecoder decodificadorDeAcesso;
  private final JwtDecoder decodificadorDeRefresh;

  public TokenJwt(JwtPropriedades propriedades, Clock clock) {
    this.propriedades = propriedades;
    this.clock = clock;

    SecretKey chave =
        new SecretKeySpec(propriedades.segredo().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    this.codificador = NimbusJwtEncoder.withSecretKey(chave).build();
    this.decodificadorDeAcesso = criarDecodificador(chave, TIPO_ACESSO);
    this.decodificadorDeRefresh = criarDecodificador(chave, TIPO_REFRESH);
  }

  public String gerarTokenDeAcesso(UUID usuarioId, String email) {
    return gerar(usuarioId, email, TIPO_ACESSO, propriedades.duracaoAcesso().getSeconds());
  }

  public String gerarTokenDeRefresh(UUID usuarioId, String email) {
    return gerar(usuarioId, email, TIPO_REFRESH, propriedades.duracaoRefresh().getSeconds());
  }

  /** Valida um refresh token e devolve o id do usuário; lança {@link JwtException} se inválido. */
  public UUID validarTokenDeRefresh(String token) {
    return UUID.fromString(decodificadorDeRefresh.decode(token).getSubject());
  }

  /** Decodificador usado pelo Spring Security em toda requisição autenticada. */
  public JwtDecoder decodificadorDeAcesso() {
    return decodificadorDeAcesso;
  }

  public long duracaoAcessoEmSegundos() {
    return propriedades.duracaoAcesso().getSeconds();
  }

  private String gerar(UUID usuarioId, String email, String tipo, long duracaoEmSegundos) {
    Instant agora = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(propriedades.emissor())
            .subject(usuarioId.toString())
            .issuedAt(agora)
            .expiresAt(agora.plusSeconds(duracaoEmSegundos))
            .id(UUID.randomUUID().toString())
            .claim(CLAIM_TIPO, tipo)
            .claim(CLAIM_EMAIL, email)
            .build();
    JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
    return codificador.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
  }

  private JwtDecoder criarDecodificador(SecretKey chave, String tipoEsperado) {
    NimbusJwtDecoder decodificador =
        NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();

    JwtTimestampValidator validadorDeValidade = new JwtTimestampValidator();
    validadorDeValidade.setClock(clock);

    OAuth2TokenValidator<Jwt> validadorDeTipo =
        jwt ->
            tipoEsperado.equals(jwt.getClaimAsString(CLAIM_TIPO))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Tipo de token inválido.", null));

    decodificador.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            validadorDeValidade, new JwtIssuerValidator(propriedades.emissor()), validadorDeTipo));
    return decodificador;
  }
}
