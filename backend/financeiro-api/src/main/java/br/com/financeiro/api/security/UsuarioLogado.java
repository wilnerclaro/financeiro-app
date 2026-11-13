package br.com.financeiro.api.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injeta no parâmetro do controller o id ({@code UUID}) do usuário dono do token JWT da requisição.
 * O cliente nunca informa o próprio id: ele vem sempre do token.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UsuarioLogado {}
