package br.com.financeiro.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Sobe um PostgreSQL descartavel para os testes de integracao.
 *
 * <p>Mesma imagem do compose.yaml. O {@link ServiceConnection} injeta URL, usuario e senha no
 * datasource, entao os testes nao dependem do banco de desenvolvimento nem do .env.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));
  }
}
