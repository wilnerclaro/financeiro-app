package br.com.financeiro.api.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Autenticação stateless com JWT. Só cadastro, login, refresh, Swagger e health são públicos; todo
 * o resto exige {@code Authorization: Bearer <token de acesso>}.
 *
 * <p>Os erros 401 e 403 passam pelo {@code GlobalExceptionHandler}, então saem no mesmo envelope
 * {@code ApiErrorResponse} do resto da API.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtPropriedades.class)
public class SecurityConfig implements WebMvcConfigurer {

  private static final String[] ROTAS_PUBLICAS = {
    "/api/auth/cadastro",
    "/api/auth/login",
    "/api/auth/refresh",
    "/actuator/health",
    "/actuator/info",
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html"
  };

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtDecoder jwtDecoder,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolvedorDeErros)
      throws Exception {
    AuthenticationEntryPoint naoAutenticado =
        (request, response, exception) ->
            resolvedorDeErros.resolveException(request, response, null, exception);
    AccessDeniedHandler semPermissao =
        (request, response, exception) ->
            resolvedorDeErros.resolveException(request, response, null, exception);

    return http.csrf(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .cors(cors -> {})
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(ROTAS_PUBLICAS)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(jwt -> jwt.decoder(jwtDecoder))
                    .authenticationEntryPoint(naoAutenticado)
                    .accessDeniedHandler(semPermissao))
        .exceptionHandling(
            erros ->
                erros.authenticationEntryPoint(naoAutenticado).accessDeniedHandler(semPermissao))
        .build();
  }

  @Bean
  public JwtDecoder jwtDecoder(TokenJwt tokenJwt) {
    return tokenJwt.decodificadorDeAcesso();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.origens-permitidas}") List<String> origensPermitidas) {
    CorsConfiguration configuracao = new CorsConfiguration();
    configuracao.setAllowedOrigins(origensPermitidas);
    configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    configuracao.setExposedHeaders(List.of("Location"));
    configuracao.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
    fonte.registerCorsConfiguration("/api/**", configuracao);
    return fonte;
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new UsuarioLogadoArgumentResolver());
  }
}
