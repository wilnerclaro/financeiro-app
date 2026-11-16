package br.com.financeiro.api.security;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.exception.NaoAutenticadoException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class VerificadorTokenGoogle {

  private static final List<String> EMISSORES =
      List.of("https://accounts.google.com", "accounts.google.com");
  private static final String TOKEN_INVALIDO =
      "Não foi possível confirmar sua conta Google. Tente de novo.";

  private final GooglePropriedades propriedades;
  private final JwtDecoder decodificador;

  public VerificadorTokenGoogle(GooglePropriedades propriedades, Clock clock) {
    this.propriedades = propriedades;
    this.decodificador = propriedades.configurado() ? criarDecodificador(clock) : null;
  }

  public IdentidadeGoogle verificar(String idToken) {
    if (decodificador == null) {
      throw new BusinessException("O login com Google não está configurado neste servidor.");
    }

    Jwt token;
    try {
      token = decodificador.decode(idToken);
    } catch (JwtException exception) {
      throw new NaoAutenticadoException(TOKEN_INVALIDO);
    }

    String email = token.getClaimAsString("email");
    if (email == null || email.isBlank() || !Boolean.TRUE.equals(emailVerificado(token))) {
      throw new NaoAutenticadoException("Sua conta Google ainda não tem o e-mail confirmado.");
    }
    return new IdentidadeGoogle(token.getSubject(), email, token.getClaimAsString("name"));
  }

  private static Boolean emailVerificado(Jwt token) {
    return Optional.ofNullable(token.getClaims().get("email_verified"))
        .map(valor -> Boolean.parseBoolean(valor.toString()))
        .orElse(false);
  }

  private JwtDecoder criarDecodificador(Clock clock) {
    NimbusJwtDecoder decodificadorGoogle =
        NimbusJwtDecoder.withJwkSetUri(propriedades.urlChaves()).build();

    JwtTimestampValidator validadorDeValidade = new JwtTimestampValidator();
    validadorDeValidade.setClock(clock);

    OAuth2TokenValidator<Jwt> validadorDePublico =
        jwt ->
            jwt.getAudience() != null && jwt.getAudience().contains(propriedades.clientId())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Token emitido para outro app.", null));

    decodificadorGoogle.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            validadorDeValidade,
            new JwtClaimValidator<String>(JwtClaimNames.ISS, EMISSORES::contains),
            new JwtClaimValidator<String>(JwtClaimNames.SUB, sub -> sub != null && !sub.isBlank()),
            validadorDePublico));
    return decodificadorGoogle;
  }
}
