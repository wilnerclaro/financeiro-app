package br.com.financeiro.api.common.exception;

public class ConflitoException extends RuntimeException {

  public ConflitoException(String message) {
    super(message);
  }
}
