package br.com.financeiro.api.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
