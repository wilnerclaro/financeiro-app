package br.com.financeiro.api.autenticacao.docs;

import br.com.financeiro.api.autenticacao.dto.CadastroRequest;
import br.com.financeiro.api.autenticacao.dto.LoginRequest;
import br.com.financeiro.api.autenticacao.dto.RefreshTokenRequest;
import br.com.financeiro.api.autenticacao.dto.TokenResponse;
import br.com.financeiro.api.autenticacao.dto.UsuarioResponse;
import br.com.financeiro.api.security.UsuarioLogado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Autenticação", description = "Cadastro, login e renovação do token")
public interface AutenticacaoApiDoc {

  @Operation(
      summary = "Cadastrar usuário",
      description = "Cria o usuário e já devolve os tokens, sem precisar de um login em seguida.")
  @SecurityRequirements
  @ApiResponse(responseCode = "201", description = "Usuário cadastrado")
  @ApiResponse(responseCode = "400", description = "Dados inválidos ou e-mail já cadastrado")
  ResponseEntity<TokenResponse> cadastrar(@Valid @RequestBody CadastroRequest request);

  @Operation(summary = "Entrar", description = "Troca e-mail e senha por um par de tokens.")
  @SecurityRequirements
  @ApiResponse(responseCode = "200", description = "Login efetuado")
  @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos, ou usuário inativo")
  ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request);

  @Operation(
      summary = "Renovar tokens",
      description = "Troca um refresh token válido por um novo par de tokens.")
  @SecurityRequirements
  @ApiResponse(responseCode = "200", description = "Tokens renovados")
  @ApiResponse(responseCode = "401", description = "Refresh token inválido ou expirado")
  ResponseEntity<TokenResponse> renovar(@Valid @RequestBody RefreshTokenRequest request);

  @Operation(summary = "Usuário logado", description = "Devolve os dados do dono do token.")
  @ApiResponse(responseCode = "200", description = "Usuário encontrado")
  @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado")
  ResponseEntity<UsuarioResponse> eu(@UsuarioLogado UUID usuarioId);
}
