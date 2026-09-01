package br.com.financeiro.api.conta.docs;

import br.com.financeiro.api.conta.dto.AtualizarContaRequest;
import br.com.financeiro.api.conta.dto.ContaResponse;
import br.com.financeiro.api.conta.dto.CorrigirSaldoInicialContaRequest;
import br.com.financeiro.api.conta.dto.CriarContaRequest;
import br.com.financeiro.api.conta.enums.TipoConta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Contas", description = "Gerenciamento de contas financeiras")
public interface ContaApiDoc {

  @Operation(summary = "Criar conta", description = "Cria uma conta financeira para um usuario.")
  @ApiResponse(responseCode = "201", description = "Conta criada com sucesso")
  @ApiResponse(responseCode = "400", description = "Requisicao invalida ou conta duplicada")
  @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
  ResponseEntity<ContaResponse> criar(@Valid @RequestBody CriarContaRequest request);

  @Operation(
      summary = "Listar contas",
      description = "Lista contas por usuario com filtros opcionais.")
  @ApiResponse(responseCode = "200", description = "Contas listadas com sucesso")
  @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
  ResponseEntity<Page<ContaResponse>> listar(
      @RequestParam UUID usuarioId,
      @RequestParam(required = false) TipoConta tipoConta,
      @RequestParam(required = false) Boolean ativa,
      Pageable pageable);

  @Operation(summary = "Buscar conta", description = "Busca uma conta pelo id e usuario.")
  @ApiResponse(responseCode = "200", description = "Conta encontrada")
  @ApiResponse(responseCode = "404", description = "Conta nao encontrada")
  ResponseEntity<ContaResponse> buscarPorId(@PathVariable UUID id, @RequestParam UUID usuarioId);

  @Operation(
      summary = "Atualizar conta",
      description = "Atualiza nome, tipo e status ativo da conta.")
  @ApiResponse(responseCode = "200", description = "Conta atualizada com sucesso")
  @ApiResponse(responseCode = "400", description = "Requisicao invalida ou conta duplicada")
  @ApiResponse(responseCode = "404", description = "Conta nao encontrada")
  ResponseEntity<ContaResponse> atualizar(
      @PathVariable UUID id, @Valid @RequestBody AtualizarContaRequest request);

  @Operation(
      summary = "Corrigir saldo inicial",
      description =
          "Altera o saldo inicial e recalcula o saldo atual com base nos lancamentos pagos.")
  @ApiResponse(responseCode = "200", description = "Saldo inicial corrigido com sucesso")
  @ApiResponse(responseCode = "404", description = "Conta nao encontrada")
  ResponseEntity<ContaResponse> corrigirSaldoInicial(
      @PathVariable UUID id, @Valid @RequestBody CorrigirSaldoInicialContaRequest request);

  @Operation(summary = "Inativar conta", description = "Inativa uma conta financeira.")
  @ApiResponse(responseCode = "204", description = "Conta inativada com sucesso")
  @ApiResponse(responseCode = "400", description = "Conta ja esta inativa")
  @ApiResponse(responseCode = "404", description = "Conta nao encontrada")
  ResponseEntity<Void> inativar(@PathVariable UUID id, @RequestParam UUID usuarioId);

  @Operation(summary = "Ativar conta", description = "Reativa uma conta financeira.")
  @ApiResponse(responseCode = "200", description = "Conta ativada com sucesso")
  @ApiResponse(responseCode = "400", description = "Conta ja esta ativa")
  @ApiResponse(responseCode = "404", description = "Conta nao encontrada")
  ResponseEntity<ContaResponse> ativar(@PathVariable UUID id, @RequestParam UUID usuarioId);
}
