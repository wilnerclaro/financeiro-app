package br.com.financeiro.api.configuracaoinicial.controller;

import br.com.financeiro.api.configuracaoinicial.docs.ConfiguracaoInicialApiDoc;
import br.com.financeiro.api.configuracaoinicial.dto.AtualizarConfiguracaoInicialRequest;
import br.com.financeiro.api.configuracaoinicial.dto.ConfiguracaoInicialResponse;
import br.com.financeiro.api.configuracaoinicial.service.ConfiguracaoInicialService;
import br.com.financeiro.api.security.UsuarioLogado;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracao-inicial")
@RequiredArgsConstructor
public class ConfiguracaoInicialController implements ConfiguracaoInicialApiDoc {

  private final ConfiguracaoInicialService service;

  @GetMapping
  public ResponseEntity<ConfiguracaoInicialResponse> buscar(@UsuarioLogado UUID usuarioId) {
    return ResponseEntity.ok(service.buscar(usuarioId));
  }

  @PutMapping
  public ResponseEntity<ConfiguracaoInicialResponse> atualizar(
      @UsuarioLogado UUID usuarioId,
      @Valid @RequestBody AtualizarConfiguracaoInicialRequest request) {
    return ResponseEntity.ok(service.atualizar(usuarioId, request));
  }
}
