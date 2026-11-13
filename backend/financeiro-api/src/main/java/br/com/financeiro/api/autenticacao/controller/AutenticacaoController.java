package br.com.financeiro.api.autenticacao.controller;

import br.com.financeiro.api.autenticacao.docs.AutenticacaoApiDoc;
import br.com.financeiro.api.autenticacao.dto.CadastroRequest;
import br.com.financeiro.api.autenticacao.dto.LoginRequest;
import br.com.financeiro.api.autenticacao.dto.RefreshTokenRequest;
import br.com.financeiro.api.autenticacao.dto.TokenResponse;
import br.com.financeiro.api.autenticacao.dto.UsuarioResponse;
import br.com.financeiro.api.autenticacao.service.AutenticacaoService;
import br.com.financeiro.api.security.UsuarioLogado;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AutenticacaoController implements AutenticacaoApiDoc {

  private final AutenticacaoService autenticacaoService;

  @PostMapping("/cadastro")
  public ResponseEntity<TokenResponse> cadastrar(@Valid @RequestBody CadastroRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(autenticacaoService.cadastrar(request));
  }

  @PostMapping("/login")
  public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(autenticacaoService.login(request));
  }

  @PostMapping("/refresh")
  public ResponseEntity<TokenResponse> renovar(@Valid @RequestBody RefreshTokenRequest request) {
    return ResponseEntity.ok(autenticacaoService.renovar(request));
  }

  @GetMapping("/eu")
  public ResponseEntity<UsuarioResponse> eu(@UsuarioLogado UUID usuarioId) {
    return ResponseEntity.ok(autenticacaoService.buscarUsuarioLogado(usuarioId));
  }
}
