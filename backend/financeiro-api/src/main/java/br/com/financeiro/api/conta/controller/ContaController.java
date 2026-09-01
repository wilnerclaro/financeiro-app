package br.com.financeiro.api.conta.controller;

import br.com.financeiro.api.conta.docs.ContaApiDoc;
import br.com.financeiro.api.conta.dto.AtualizarContaRequest;
import br.com.financeiro.api.conta.dto.ContaResponse;
import br.com.financeiro.api.conta.dto.CorrigirSaldoInicialContaRequest;
import br.com.financeiro.api.conta.dto.CriarContaRequest;
import br.com.financeiro.api.conta.enums.TipoConta;
import br.com.financeiro.api.conta.service.ContaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contas")
@RequiredArgsConstructor
public class ContaController implements ContaApiDoc {

  private final ContaService contaService;

  @PostMapping
  public ResponseEntity<ContaResponse> criar(@Valid @RequestBody CriarContaRequest request) {
    ContaResponse response = contaService.criar(request);
    URI location = URI.create("/api/contas/" + response.id());

    return ResponseEntity.created(location).body(response);
  }

  @GetMapping
  public ResponseEntity<Page<ContaResponse>> listar(
      @RequestParam UUID usuarioId,
      @RequestParam(required = false) TipoConta tipoConta,
      @RequestParam(required = false) Boolean ativa,
      @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC)
          Pageable pageable) {
    Page<ContaResponse> response = contaService.listar(usuarioId, tipoConta, ativa, pageable);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<ContaResponse> buscarPorId(
      @PathVariable UUID id, @RequestParam UUID usuarioId) {
    ContaResponse response = contaService.buscarPorId(id, usuarioId);
    return ResponseEntity.ok(response);
  }

  @PutMapping("/{id}")
  public ResponseEntity<ContaResponse> atualizar(
      @PathVariable UUID id, @Valid @RequestBody AtualizarContaRequest request) {
    ContaResponse response = contaService.atualizar(id, request);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}/corrigir-saldo-inicial")
  public ResponseEntity<ContaResponse> corrigirSaldoInicial(
      @PathVariable UUID id, @Valid @RequestBody CorrigirSaldoInicialContaRequest request) {
    ContaResponse response = contaService.corrigirSaldoInicial(id, request);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> inativar(@PathVariable UUID id, @RequestParam UUID usuarioId) {
    contaService.inativar(id, usuarioId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/ativar")
  public ResponseEntity<ContaResponse> ativar(@PathVariable UUID id, @RequestParam UUID usuarioId) {
    ContaResponse response = contaService.ativar(id, usuarioId);
    return ResponseEntity.ok(response);
  }
}
