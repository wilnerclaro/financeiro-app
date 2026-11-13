package br.com.financeiro.api.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio da aplicação. As regras de "hoje" (status atrasado, pagamento no futuro) usam o fuso do
 * Brasil, independentemente do fuso do servidor. Nos testes, este bean pode ser substituído por um
 * {@link Clock#fixed} para controlar a data.
 */
@Configuration
public class ClockConfig {

  public static final ZoneId FUSO_PADRAO = ZoneId.of("America/Sao_Paulo");

  @Bean
  public Clock clock() {
    return Clock.system(FUSO_PADRAO);
  }
}
