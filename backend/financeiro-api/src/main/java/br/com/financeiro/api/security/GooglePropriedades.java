package br.com.financeiro.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.google")
public record GooglePropriedades(String clientId, String urlChaves) {

  public boolean configurado() {
    return clientId != null && !clientId.isBlank();
  }
}
