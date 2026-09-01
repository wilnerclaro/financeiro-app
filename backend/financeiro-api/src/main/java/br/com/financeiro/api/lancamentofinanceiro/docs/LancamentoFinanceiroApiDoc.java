package br.com.financeiro.api.lancamentofinanceiro.docs;

import br.com.financeiro.api.lancamentofinanceiro.dto.AtualizarLancamentoFinanceiroRequest;
import br.com.financeiro.api.lancamentofinanceiro.dto.CriarLancamentoFinanceiroRequest;
import br.com.financeiro.api.lancamentofinanceiro.dto.LancamentoFinanceiroResponse;
import br.com.financeiro.api.lancamentofinanceiro.dto.PagarLancamentoFinanceiroRequest;
import br.com.financeiro.api.lancamentofinanceiro.enums.StatusLancamentoFinanceiro;
import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Lancamentos Financeiros", description = "Gerenciamento de receitas e despesas")
public interface LancamentoFinanceiroApiDoc {

  @Operation(
      summary = "Criar lancamento",
      description = "Cria uma receita ou despesa. Lancamentos pagos movimentam o saldo da conta.")
  @ApiResponse(responseCode = "201", description = "Lancamento criado com sucesso")
  @ApiResponse(
      responseCode = "400",
      description = "Requisicao invalida ou regra de negocio violada")
  @ApiResponse(responseCode = "404", description = "Usuario, conta ou categoria nao encontrada")
  ResponseEntity<LancamentoFinanceiroResponse> criar(
      @Valid @RequestBody CriarLancamentoFinanceiroRequest request);

  @Operation(
      summary = "Listar lancamentos",
      description = "Lista lancamentos por usuario com filtros opcionais.")
  @ApiResponse(responseCode = "200", description = "Lancamentos listados com sucesso")
  @ApiResponse(responseCode = "400", description = "Periodo invalido")
  @ApiResponse(responseCode = "404", description = "Usuario, conta ou categoria nao encontrada")
  ResponseEntity<Page<LancamentoFinanceiroResponse>> listar(
      @RequestParam UUID usuarioId,
      @RequestParam(required = false) TipoLancamentoFinanceiro tipo,
      @RequestParam(required = false) StatusLancamentoFinanceiro status,
      @RequestParam(required = false) UUID contaId,
      @RequestParam(required = false) UUID categoriaId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataInicio,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataFim,
      Pageable pageable);

  @Operation(
      summary = "Buscar lancamento",
      description = "Busca um lancamento financeiro pelo id e usuario.")
  @ApiResponse(responseCode = "200", description = "Lancamento encontrado")
  @ApiResponse(responseCode = "404", description = "Lancamento nao encontrado")
  ResponseEntity<LancamentoFinanceiroResponse> buscarPorId(
      @PathVariable UUID id, @RequestParam UUID usuarioId);

  @Operation(
      summary = "Atualizar lancamento",
      description = "Atualiza um lancamento e recalcula o saldo quando necessario.")
  @ApiResponse(responseCode = "200", description = "Lancamento atualizado com sucesso")
  @ApiResponse(
      responseCode = "400",
      description = "Requisicao invalida ou regra de negocio violada")
  @ApiResponse(responseCode = "404", description = "Lancamento, conta ou categoria nao encontrada")
  ResponseEntity<LancamentoFinanceiroResponse> atualizar(
      @PathVariable UUID id, @Valid @RequestBody AtualizarLancamentoFinanceiroRequest request);

  @Operation(
      summary = "Pagar lancamento",
      description = "Marca um lancamento como pago e movimenta o saldo da conta.")
  @ApiResponse(responseCode = "200", description = "Lancamento pago com sucesso")
  @ApiResponse(responseCode = "400", description = "Lancamento ja pago, cancelado ou data invalida")
  @ApiResponse(responseCode = "404", description = "Lancamento nao encontrado")
  ResponseEntity<LancamentoFinanceiroResponse> pagar(
      @PathVariable UUID id, @Valid @RequestBody PagarLancamentoFinanceiroRequest request);

  @Operation(
      summary = "Cancelar lancamento",
      description = "Cancela um lancamento. Se estiver pago, o saldo e estornado.")
  @ApiResponse(responseCode = "204", description = "Lancamento cancelado com sucesso")
  @ApiResponse(responseCode = "400", description = "Lancamento ja cancelado")
  @ApiResponse(responseCode = "404", description = "Lancamento nao encontrado")
  ResponseEntity<Void> cancelar(@PathVariable UUID id, @RequestParam UUID usuarioId);
}
