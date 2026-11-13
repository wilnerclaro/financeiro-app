package br.com.financeiro.api.config;

import br.com.financeiro.api.security.UsuarioLogado;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String ESQUEMA_JWT = "bearer-jwt";

  static {
    // O id do usuário vem do token, então não aparece como parâmetro no Swagger.
    SpringDocUtils.getConfig().addAnnotationsToIgnore(UsuarioLogado.class);
  }

  @Bean
  public OpenAPI financeiroOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Financeiro API")
                .description(
                    "API REST para sistema financeiro pessoal e profissional. Faça login em "
                        + "/api/auth/login e use o accessToken no botão Authorize.")
                .version("v1"))
        .components(
            new Components()
                .addSecuritySchemes(
                    ESQUEMA_JWT,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
  }
}
