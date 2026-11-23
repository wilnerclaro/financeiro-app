package br.com.financeiro.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.exception.NaoAutenticadoException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Verificação do ID token do Google")
class VerificadorTokenGoogleTest {

  private static final String CLIENT_ID = "app.apps.googleusercontent.com";
  private static final Instant AGORA = Instant.parse("2026-11-15T12:00:00Z");
  private static final String RECUSA =
      "Não foi possível confirmar sua conta Google. Tente de novo.";

  private static HttpServer servidorDeChaves;
  private static RSAKey chaveDoGoogle;
  private static RSAKey chaveDeOutro;
  private static VerificadorTokenGoogle verificador;

  @BeforeAll
  static void subirChavesDoGoogle() throws Exception {
    chaveDoGoogle = new RSAKeyGenerator(2048).keyID("google").generate();
    chaveDeOutro = new RSAKeyGenerator(2048).keyID("google").generate();
    byte[] jwks =
        new JWKSet(chaveDoGoogle.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);

    servidorDeChaves = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    servidorDeChaves.createContext(
        "/certs",
        troca -> {
          troca.getResponseHeaders().add("Content-Type", "application/json");
          troca.sendResponseHeaders(200, jwks.length);
          troca.getResponseBody().write(jwks);
          troca.close();
        });
    servidorDeChaves.start();

    String url = "http://localhost:" + servidorDeChaves.getAddress().getPort() + "/certs";
    verificador =
        new VerificadorTokenGoogle(
            new GooglePropriedades(CLIENT_ID, url), Clock.fixed(AGORA, ZoneOffset.UTC));
  }

  @AfterAll
  static void derrubarServidor() {
    servidorDeChaves.stop(0);
  }

  @ParameterizedTest
  @ValueSource(strings = {"https://accounts.google.com", "accounts.google.com"})
  @DisplayName("token válido devolve sujeito, e-mail e nome")
  void tokenValido(String emissor) throws Exception {
    IdentidadeGoogle identidade = verificador.verificar(token(c -> c.issuer(emissor)));

    assertThat(identidade).isEqualTo(new IdentidadeGoogle("123", "ana@gmail.com", "Ana"));
  }

  @Test
  @DisplayName("email_verified como texto \"true\" também vale")
  void emailVerificadoComoTexto() throws Exception {
    assertThat(verificador.verificar(token(c -> c.claim("email_verified", "true"))).email())
        .isEqualTo("ana@gmail.com");
  }

  @Test
  @DisplayName(
      "token de outro app, de outro emissor, vencido ou assinado por outra chave é recusado")
  void tokensRecusados() throws Exception {
    assertRecusado(token(c -> c.audience("outro-app")));
    assertRecusado(token(c -> c.issuer("https://emissor-falso.com")));
    assertRecusado(token(c -> c.expirationTime(Date.from(AGORA.minusSeconds(3600)))));
    assertRecusado(token(c -> c.subject(null)));
    assertRecusado(assinar(chaveDeOutro, claims(c -> {})));
    assertRecusado("isto-nao-e-um-jwt");
  }

  @Test
  @DisplayName("e-mail não confirmado ou ausente é recusado")
  void emailNaoConfirmado() throws Exception {
    String mensagem = "Sua conta Google ainda não tem o e-mail confirmado.";

    assertThatThrownBy(() -> verificador.verificar(token(c -> c.claim("email_verified", false))))
        .isInstanceOf(NaoAutenticadoException.class)
        .hasMessage(mensagem);
    assertThatThrownBy(() -> verificador.verificar(token(c -> c.claim("email_verified", null))))
        .hasMessage(mensagem);
    assertThatThrownBy(() -> verificador.verificar(token(c -> c.claim("email", null))))
        .hasMessage(mensagem);
    assertThatThrownBy(() -> verificador.verificar(token(c -> c.claim("email", " "))))
        .hasMessage(mensagem);
  }

  @Test
  @DisplayName("sem Client ID configurado, o login com Google fica desligado")
  void naoConfigurado() {
    GooglePropriedades vazio = new GooglePropriedades(" ", "http://localhost/certs");
    VerificadorTokenGoogle desligado = new VerificadorTokenGoogle(vazio, Clock.systemUTC());

    assertThat(vazio.configurado()).isFalse();
    assertThat(new GooglePropriedades(null, null).configurado()).isFalse();
    assertThatThrownBy(() -> desligado.verificar("qualquer"))
        .isInstanceOf(BusinessException.class)
        .hasMessage("O login com Google não está configurado neste servidor.");
  }

  private static void assertRecusado(String token) {
    assertThatThrownBy(() -> verificador.verificar(token))
        .isInstanceOf(NaoAutenticadoException.class)
        .hasMessage(RECUSA);
  }

  private static String token(Consumer<JWTClaimsSet.Builder> ajuste) throws Exception {
    return assinar(chaveDoGoogle, claims(ajuste));
  }

  private static JWTClaimsSet claims(Consumer<JWTClaimsSet.Builder> ajuste) {
    JWTClaimsSet.Builder claims =
        new JWTClaimsSet.Builder()
            .issuer("https://accounts.google.com")
            .audience(CLIENT_ID)
            .subject("123")
            .issueTime(Date.from(AGORA.minusSeconds(60)))
            .expirationTime(Date.from(AGORA.plusSeconds(3600)))
            .claim("email", "ana@gmail.com")
            .claim("email_verified", true)
            .claim("name", "Ana");
    ajuste.accept(claims);
    return claims.build();
  }

  private static String assinar(RSAKey chave, JWTClaimsSet claims) throws Exception {
    SignedJWT jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(chave.getKeyID())
                .type(JOSEObjectType.JWT)
                .build(),
            claims);
    jwt.sign(new RSASSASigner(chave));
    return jwt.serialize();
  }
}
