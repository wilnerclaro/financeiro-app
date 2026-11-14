package br.com.financeiro.api.common.exception;

public class NaoAutenticadoException extends RuntimeException {

  public NaoAutenticadoException(String message) {
    super(message);
  }
}
