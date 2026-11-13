package br.com.financeiro.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI financeiroOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Financeiro API")
                .description("API REST para sistema financeiro pessoal e profissional")
                .version("v1"));
  }
}
