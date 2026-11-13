package br.com.financeiro.api.lancamentofinanceiro.docs;

import br.com.financeiro.api.lancamentofinanceiro.dto.AtualizarLancamentoFinanceiroRequest;
import br.com.financeiro.api.lancamentofinanceiro.dto.CriarLancamentoFinanceiroRequest;
import br.com.financeiro.api.lancamentofinanceiro.dto.LancamentoFinanceiroResponse;
import br.com.financeiro.api.lancamentofinanceiro.dto.PagarLancamentoFinanceiroRequest;
import br.com.financeiro.api.lancamentofinanceiro.enums.StatusLancamentoFinanceiro;
import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import br.com.financeiro.api.security.UsuarioLogado;
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

@Tag(name = "Lançamentos Financeiros", description = "Gerenciamento de receitas e despesas")
public interface LancamentoFinanceiroApiDoc {

  @Operation(
      summary = "Criar lançamento",
      description = "Cria uma receita ou despesa. Lançamentos pagos movimentam o saldo da conta.")
  @ApiResponse(responseCode = "201", description = "Lançamento criado com sucesso")
  @ApiResponse(
      responseCode = "400",
      description = "Requisição inválida ou regra de negócio violada")
  @ApiResponse(responseCode = "404", description = "Usuário, conta ou categoria não encontrada")
  ResponseEntity<LancamentoFinanceiroResponse> criar(
      @UsuarioLogado UUID usuarioId, @Valid @RequestBody CriarLancamentoFinanceiroRequest request);

  @Operation(
      summary = "Listar lançamentos",
      description = "Lista lançamentos por usuário com filtros opcionais.")
  @ApiResponse(responseCode = "200", description = "Lançamentos listados com sucesso")
  @ApiResponse(responseCode = "400", description = "Período inválido")
  @ApiResponse(responseCode = "404", description = "Usuário, conta ou categoria não encontrada")
  ResponseEntity<Page<LancamentoFinanceiroResponse>> listar(
      @UsuarioLogado UUID usuarioId,
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
      summary = "Buscar lançamento",
      description = "Busca um lançamento financeiro pelo id e usuário.")
  @ApiResponse(responseCode = "200", description = "Lançamento encontrado")
  @ApiResponse(responseCode = "404", description = "Lançamento não encontrado")
  ResponseEntity<LancamentoFinanceiroResponse> buscarPorId(
      @PathVariable UUID id, @UsuarioLogado UUID usuarioId);

  @Operation(
      summary = "Atualizar lançamento",
      description = "Atualiza um lançamento e recalcula o saldo quando necessário.")
  @ApiResponse(responseCode = "200", description = "Lançamento atualizado com sucesso")
  @ApiResponse(
      responseCode = "400",
      description = "Requisição inválida ou regra de negócio violada")
  @ApiResponse(responseCode = "404", description = "Lançamento, conta ou categoria não encontrada")
  ResponseEntity<LancamentoFinanceiroResponse> atualizar(
      @PathVariable UUID id,
      @UsuarioLogado UUID usuarioId,
      @Valid @RequestBody AtualizarLancamentoFinanceiroRequest request);

  @Operation(
      summary = "Pagar lançamento",
      description = "Marca um lançamento como pago e movimenta o saldo da conta.")
  @ApiResponse(responseCode = "200", description = "Lançamento pago com sucesso")
  @ApiResponse(responseCode = "400", description = "Lançamento já pago, cancelado ou data inválida")
  @ApiResponse(responseCode = "404", description = "Lançamento não encontrado")
  ResponseEntity<LancamentoFinanceiroResponse> pagar(
      @PathVariable UUID id,
      @UsuarioLogado UUID usuarioId,
      @Valid @RequestBody PagarLancamentoFinanceiroRequest request);

  @Operation(
      summary = "Cancelar lançamento",
      description = "Cancela um lançamento. Se estiver pago, o saldo é estornado.")
  @ApiResponse(responseCode = "204", description = "Lançamento cancelado com sucesso")
  @ApiResponse(responseCode = "400", description = "Lançamento já cancelado")
  @ApiResponse(responseCode = "404", description = "Lançamento não encontrado")
  ResponseEntity<Void> cancelar(@PathVariable UUID id, @UsuarioLogado UUID usuarioId);
}
