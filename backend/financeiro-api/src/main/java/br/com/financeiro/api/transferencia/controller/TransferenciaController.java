package br.com.financeiro.api.transferencia.controller;

import br.com.financeiro.api.security.UsuarioLogado;
import br.com.financeiro.api.transferencia.docs.TransferenciaApiDoc;
import br.com.financeiro.api.transferencia.dto.CriarTransferenciaRequest;
import br.com.financeiro.api.transferencia.dto.TransferenciaResponse;
import br.com.financeiro.api.transferencia.service.TransferenciaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transferencias")
@RequiredArgsConstructor
public class TransferenciaController implements TransferenciaApiDoc {

  private final TransferenciaService transferenciaService;

  @PostMapping
  public ResponseEntity<TransferenciaResponse> criar(
      @UsuarioLogado UUID usuarioId, @Valid @RequestBody CriarTransferenciaRequest request) {
    TransferenciaResponse response = transferenciaService.criar(usuarioId, request);
    return ResponseEntity.created(URI.create("/api/transferencias/" + response.id()))
        .body(response);
  }

  @GetMapping
  public ResponseEntity<Page<TransferenciaResponse>> listar(
      @UsuarioLogado UUID usuarioId,
      @RequestParam(required = false) UUID contaId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataInicio,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataFim,
      @PageableDefault(size = 20, sort = "dataTransferencia", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return ResponseEntity.ok(
        transferenciaService.listar(usuarioId, contaId, dataInicio, dataFim, pageable));
  }

  @GetMapping("/{id}")
  public ResponseEntity<TransferenciaResponse> buscarPorId(
      @PathVariable UUID id, @UsuarioLogado UUID usuarioId) {
    return ResponseEntity.ok(transferenciaService.buscarPorId(id, usuarioId));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> cancelar(@PathVariable UUID id, @UsuarioLogado UUID usuarioId) {
    transferenciaService.cancelar(id, usuarioId);
    return ResponseEntity.noContent().build();
  }
}
