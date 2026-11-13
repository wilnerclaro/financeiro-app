package br.com.financeiro.api.security;

import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Resolve {@link UsuarioLogado} a partir do {@code sub} do JWT autenticado. */
public class UsuarioLogadoArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(UsuarioLogado.class)
        && UUID.class.equals(parameter.getParameterType());
  }

  @Override
  public UUID resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();

    if (autenticacao instanceof JwtAuthenticationToken token) {
      return UUID.fromString(token.getToken().getSubject());
    }

    throw new InsufficientAuthenticationException("Autenticação obrigatória.");
  }
}
