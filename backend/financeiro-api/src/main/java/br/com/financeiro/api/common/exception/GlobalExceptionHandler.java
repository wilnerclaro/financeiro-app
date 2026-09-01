package br.com.financeiro.api.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> tratarRecursoNaoEncontrado(
      ResourceNotFoundException exception, HttpServletRequest request) {
    HttpStatus status = HttpStatus.NOT_FOUND;

    ApiErrorResponse response =
        criarRespostaDeErro(status, exception.getMessage(), request, List.of());

    return ResponseEntity.status(status).body(response);
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiErrorResponse> tratarExcecaoDeNegocio(
      BusinessException exception, HttpServletRequest request) {
    HttpStatus status = HttpStatus.BAD_REQUEST;

    ApiErrorResponse response =
        criarRespostaDeErro(status, exception.getMessage(), request, List.of());

    return ResponseEntity.status(status).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> tratarErroDeValidacao(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    HttpStatus status = HttpStatus.BAD_REQUEST;

    List<FieldErrorResponse> fieldErrors =
        exception.getBindingResult().getFieldErrors().stream().map(this::paraErroDeCampo).toList();

    ApiErrorResponse response =
        criarRespostaDeErro(
            status, "Existem campos invalidos na requisicao.", request, fieldErrors);

    return ResponseEntity.status(status).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> tratarErroGenerico(
      Exception exception, HttpServletRequest request) {
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

    ApiErrorResponse response =
        criarRespostaDeErro(status, "Erro interno inesperado.", request, List.of());

    return ResponseEntity.status(status).body(response);
  }

  private ApiErrorResponse criarRespostaDeErro(
      HttpStatus status,
      String message,
      HttpServletRequest request,
      List<FieldErrorResponse> fieldErrors) {
    return new ApiErrorResponse(
        OffsetDateTime.now(),
        status.value(),
        status.getReasonPhrase(),
        message,
        request.getRequestURI(),
        fieldErrors);
  }

  private FieldErrorResponse paraErroDeCampo(FieldError fieldError) {
    return new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage());
  }
}
