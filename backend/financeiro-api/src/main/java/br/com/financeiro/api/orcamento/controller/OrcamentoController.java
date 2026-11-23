package br.com.financeiro.api.orcamento.controller;

import br.com.financeiro.api.orcamento.docs.OrcamentoApiDoc;
import br.com.financeiro.api.orcamento.dto.CopiaDePrevistoResponse;
import br.com.financeiro.api.orcamento.dto.CopiarPrevistoRequest;
import br.com.financeiro.api.orcamento.dto.DefinirOrcamentoRequest;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse;
import br.com.financeiro.api.orcamento.dto.RepetirPrevistoRequest;
import br.com.financeiro.api.orcamento.dto.ResumoAnualOrcamentoResponse;
import br.com.financeiro.api.orcamento.service.OrcamentoService;
import br.com.financeiro.api.security.UsuarioLogado;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orcamentos")
@RequiredArgsConstructor
public class OrcamentoController implements OrcamentoApiDoc {

  private final OrcamentoService orcamentoService;

  @GetMapping("/{ano}/{mes}")
  public ResponseEntity<PrevistoRealizadoResponse> previstoRealizado(
      @UsuarioLogado UUID usuarioId, @PathVariable int ano, @PathVariable int mes) {
    return ResponseEntity.ok(orcamentoService.previstoRealizado(usuarioId, ano, mes));
  }

  @PutMapping("/{ano}/{mes}")
  public ResponseEntity<PrevistoRealizadoResponse> definir(
      @UsuarioLogado UUID usuarioId,
      @PathVariable int ano,
      @PathVariable int mes,
      @Valid @RequestBody DefinirOrcamentoRequest request) {
    return ResponseEntity.ok(orcamentoService.definir(usuarioId, ano, mes, request));
  }

  @PostMapping("/{ano}/{mes}/copiar")
  public ResponseEntity<CopiaDePrevistoResponse> copiar(
      @UsuarioLogado UUID usuarioId,
      @PathVariable int ano,
      @PathVariable int mes,
      @Valid @RequestBody CopiarPrevistoRequest request) {
    return ResponseEntity.ok(orcamentoService.copiar(usuarioId, ano, mes, request));
  }

  @PostMapping("/repetir")
  public ResponseEntity<CopiaDePrevistoResponse> repetir(
      @UsuarioLogado UUID usuarioId, @Valid @RequestBody RepetirPrevistoRequest request) {
    return ResponseEntity.ok(orcamentoService.repetir(usuarioId, request));
  }

  @GetMapping("/{ano}")
  public ResponseEntity<ResumoAnualOrcamentoResponse> resumoAnual(
      @UsuarioLogado UUID usuarioId, @PathVariable int ano) {
    return ResponseEntity.ok(orcamentoService.resumoAnual(usuarioId, ano));
  }
}
