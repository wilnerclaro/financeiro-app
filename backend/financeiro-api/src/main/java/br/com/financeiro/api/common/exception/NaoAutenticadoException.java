package br.com.financeiro.api.common.exception;

/** Credenciais inválidas no login ou no refresh. Vira 401 no {@link GlobalExceptionHandler}. */
public class NaoAutenticadoException extends RuntimeException {

  public NaoAutenticadoException(String message) {
    super(message);
  }
}
