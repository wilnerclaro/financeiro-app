package br.com.financeiro.api.meta.controller;

import br.com.financeiro.api.meta.docs.MetaApiDoc;
import br.com.financeiro.api.meta.dto.AtualizarMetaRequest;
import br.com.financeiro.api.meta.dto.CriarMetaRequest;
import br.com.financeiro.api.meta.dto.MetaResponse;
import br.com.financeiro.api.meta.dto.MovimentoMetaRequest;
import br.com.financeiro.api.meta.dto.PainelMetasResponse;
import br.com.financeiro.api.meta.service.MetaService;
import br.com.financeiro.api.security.UsuarioLogado;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metas")
@RequiredArgsConstructor
public class MetaController implements MetaApiDoc {

  private final MetaService metaService;

  @GetMapping
  public ResponseEntity<PainelMetasResponse> painel(
      @UsuarioLogado UUID usuarioId,
      @RequestParam(required = false) Integer ano,
      @RequestParam(defaultValue = "false") boolean incluirInativas) {
    return ResponseEntity.ok(metaService.painel(usuarioId, ano, incluirInativas));
  }

  @PostMapping
  public ResponseEntity<MetaResponse> criar(
      @UsuarioLogado UUID usuarioId, @Valid @RequestBody CriarMetaRequest request) {
    MetaResponse response = metaService.criar(usuarioId, request);
    return ResponseEntity.created(URI.create("/api/metas/" + response.id())).body(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<MetaResponse> buscar(
      @PathVariable UUID id,
      @UsuarioLogado UUID usuarioId,
      @RequestParam(required = false) Integer ano) {
    return ResponseEntity.ok(metaService.buscar(id, usuarioId, ano));
  }

  @PutMapping("/{id}")
  public ResponseEntity<MetaResponse> atualizar(
      @PathVariable UUID id,
      @UsuarioLogado UUID usuarioId,
      @Valid @RequestBody AtualizarMetaRequest request) {
    return ResponseEntity.ok(metaService.atualizar(id, usuarioId, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> inativar(@PathVariable UUID id, @UsuarioLogado UUID usuarioId) {
    metaService.inativar(id, usuarioId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/ativar")
  public ResponseEntity<MetaResponse> ativar(@PathVariable UUID id, @UsuarioLogado UUID usuarioId) {
    return ResponseEntity.ok(metaService.ativar(id, usuarioId));
  }

  @PutMapping("/{id}/movimentos/{ano}/{mes}")
  public ResponseEntity<MetaResponse> registrarMovimento(
      @PathVariable UUID id,
      @UsuarioLogado UUID usuarioId,
      @PathVariable int ano,
      @PathVariable int mes,
      @Valid @RequestBody MovimentoMetaRequest request) {
    return ResponseEntity.ok(metaService.registrarMovimento(id, usuarioId, ano, mes, request));
  }
}
