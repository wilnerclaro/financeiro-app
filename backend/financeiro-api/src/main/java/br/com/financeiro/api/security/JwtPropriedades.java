package br.com.financeiro.api.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração dos tokens ({@code app.jwt.*}). O segredo vem da variável {@code JWT_SECRET} do
 * {@code .env} e precisa ter pelo menos 32 bytes, que é o mínimo do HMAC-SHA256.
 */
@ConfigurationProperties("app.jwt")
public record JwtPropriedades(
    String segredo, String emissor, Duration duracaoAcesso, Duration duracaoRefresh) {

  private static final int TAMANHO_MINIMO_SEGREDO = 32;

  public JwtPropriedades {
    if (segredo == null
        || segredo.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SEGREDO) {
      throw new IllegalStateException(
          "Defina JWT_SECRET no .env com pelo menos 32 caracteres "
              + "(por exemplo, a saída de: openssl rand -base64 48).");
    }
  }
}
