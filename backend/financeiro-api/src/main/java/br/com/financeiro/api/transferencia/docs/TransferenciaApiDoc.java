package br.com.financeiro.api.transferencia.docs;

import br.com.financeiro.api.security.UsuarioLogado;
import br.com.financeiro.api.transferencia.dto.CriarTransferenciaRequest;
import br.com.financeiro.api.transferencia.dto.TransferenciaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(
    name = "Transferências",
    description = "Dinheiro entre contas do usuário, inclusive o pagamento da fatura do cartão")
public interface TransferenciaApiDoc {

  @Operation(
      summary = "Transferir",
      description =
          "Tira o valor da conta de origem e põe na de destino. Não conta como receita nem "
              + "despesa. Para pagar a fatura do cartão, use o cartão como destino.")
  @ApiResponse(responseCode = "201", description = "Transferência feita")
  @ApiResponse(responseCode = "400", description = "Contas iguais, conta inativa ou data futura")
  @ApiResponse(responseCode = "404", description = "Conta não encontrada")
  ResponseEntity<TransferenciaResponse> criar(
      @UsuarioLogado UUID usuarioId, @Valid @RequestBody CriarTransferenciaRequest request);

  @Operation(
      summary = "Listar transferências",
      description = "Filtra por conta (origem ou destino) e por período.")
  @ApiResponse(responseCode = "200", description = "Transferências listadas")
  ResponseEntity<Page<TransferenciaResponse>> listar(
      @UsuarioLogado UUID usuarioId,
      @RequestParam(required = false) UUID contaId,
      @RequestParam(required = false) LocalDate dataInicio,
      @RequestParam(required = false) LocalDate dataFim,
      Pageable pageable);

  @Operation(summary = "Buscar transferência")
  @ApiResponse(responseCode = "200", description = "Transferência encontrada")
  @ApiResponse(responseCode = "404", description = "Transferência não encontrada")
  ResponseEntity<TransferenciaResponse> buscarPorId(
      @PathVariable UUID id, @UsuarioLogado UUID usuarioId);

  @Operation(
      summary = "Cancelar transferência",
      description = "Cancela e devolve o valor para a conta de origem.")
  @ApiResponse(responseCode = "204", description = "Transferência cancelada")
  @ApiResponse(responseCode = "400", description = "Transferência já cancelada")
  @ApiResponse(responseCode = "404", description = "Transferência não encontrada")
  ResponseEntity<Void> cancelar(@PathVariable UUID id, @UsuarioLogado UUID usuarioId);
}
