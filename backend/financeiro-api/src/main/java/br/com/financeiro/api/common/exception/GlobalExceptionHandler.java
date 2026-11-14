package br.com.financeiro.api.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final Clock clock;

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> tratarRecursoNaoEncontrado(
      ResourceNotFoundException exception, HttpServletRequest request) {
    return responder(HttpStatus.NOT_FOUND, exception.getMessage(), request);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiErrorResponse> tratarRotaNaoEncontrada(
      NoResourceFoundException exception, HttpServletRequest request) {
    return responder(HttpStatus.NOT_FOUND, "Recurso não encontrado.", request);
  }

  @ExceptionHandler(NaoAutenticadoException.class)
  public ResponseEntity<ApiErrorResponse> tratarCredenciaisInvalidas(
      NaoAutenticadoException exception, HttpServletRequest request) {
    return responder(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiErrorResponse> tratarNaoAutenticado(
      AuthenticationException exception, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .header("WWW-Authenticate", "Bearer")
        .body(
            criarRespostaDeErro(
                HttpStatus.UNAUTHORIZED,
                "Token de acesso ausente, inválido ou expirado.",
                request,
                List.of()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiErrorResponse> tratarAcessoNegado(
      AccessDeniedException exception, HttpServletRequest request) {
    return responder(HttpStatus.FORBIDDEN, "Acesso negado.", request);
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiErrorResponse> tratarExcecaoDeNegocio(
      BusinessException exception, HttpServletRequest request) {
    return responder(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> tratarErroDeValidacao(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    HttpStatus status = HttpStatus.BAD_REQUEST;

    List<FieldErrorResponse> fieldErrors =
        exception.getBindingResult().getFieldErrors().stream().map(this::paraErroDeCampo).toList();

    ApiErrorResponse response =
        criarRespostaDeErro(
            status, "Existem campos inválidos na requisição.", request, fieldErrors);

    return ResponseEntity.status(status).body(response);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> tratarCorpoInvalido(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    log.debug("Corpo da requisição inválido em {}", request.getRequestURI(), exception);
    return responder(
        HttpStatus.BAD_REQUEST,
        "O corpo da requisição está ausente ou com formato inválido.",
        request);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiErrorResponse> tratarParametroComTipoInvalido(
      MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
    return responder(
        HttpStatus.BAD_REQUEST,
        "O parâmetro '%s' possui um valor inválido.".formatted(exception.getName()),
        request);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiErrorResponse> tratarParametroAusente(
      MissingServletRequestParameterException exception, HttpServletRequest request) {
    return responder(
        HttpStatus.BAD_REQUEST,
        "O parâmetro '%s' é obrigatório.".formatted(exception.getParameterName()),
        request);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiErrorResponse> tratarMetodoNaoSuportado(
      HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
    return responder(
        HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não suportado para este recurso.", request);
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ApiErrorResponse> tratarConflitoDeConcorrencia(
      ObjectOptimisticLockingFailureException exception, HttpServletRequest request) {
    log.info("Conflito de concorrência em {}: {}", request.getRequestURI(), exception.getMessage());
    return responder(
        HttpStatus.CONFLICT,
        "Os dados foram alterados por outra operação ao mesmo tempo. Tente novamente.",
        request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiErrorResponse> tratarViolacaoDeIntegridade(
      DataIntegrityViolationException exception, HttpServletRequest request) {
    log.warn(
        "Violação de integridade em {}: {}",
        request.getRequestURI(),
        exception.getMostSpecificCause().getMessage());
    return responder(
        HttpStatus.CONFLICT,
        "A operação conflita com dados já existentes (por exemplo, um registro duplicado).",
        request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> tratarErroGenerico(
      Exception exception, HttpServletRequest request) {
    log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), exception);
    return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado.", request);
  }

  private ResponseEntity<ApiErrorResponse> responder(
      HttpStatus status, String message, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(criarRespostaDeErro(status, message, request, List.of()));
  }

  private ApiErrorResponse criarRespostaDeErro(
      HttpStatus status,
      String message,
      HttpServletRequest request,
      List<FieldErrorResponse> fieldErrors) {
    return new ApiErrorResponse(
        OffsetDateTime.now(clock),
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
