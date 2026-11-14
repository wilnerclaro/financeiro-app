package br.com.financeiro.api.meta.docs;

import br.com.financeiro.api.meta.dto.AtualizarMetaRequest;
import br.com.financeiro.api.meta.dto.CriarMetaRequest;
import br.com.financeiro.api.meta.dto.MetaResponse;
import br.com.financeiro.api.meta.dto.MovimentoMetaRequest;
import br.com.financeiro.api.meta.dto.PainelMetasResponse;
import br.com.financeiro.api.security.UsuarioLogado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Metas", description = "Metas e sonhos: quanto juntar, aportes, juros e acumulado")
public interface MetaApiDoc {

  @Operation(
      summary = "Painel de metas do ano",
      description =
          "Todas as metas com acumulado, percentual atingido e quanto falta, os 12 meses do ano "
              + "e a linha de totais. Sem ano, usa o ano atual.")
  @ApiResponse(responseCode = "200", description = "Painel do ano")
  ResponseEntity<PainelMetasResponse> painel(
      @UsuarioLogado UUID usuarioId,
      @RequestParam(required = false) Integer ano,
      @RequestParam(defaultValue = "false") boolean incluirInativas);

  @Operation(summary = "Criar meta")
  @ApiResponse(responseCode = "201", description = "Meta criada")
  @ApiResponse(responseCode = "400", description = "Dados inválidos ou nome repetido")
  ResponseEntity<MetaResponse> criar(
      @UsuarioLogado UUID usuarioId, @Valid @RequestBody CriarMetaRequest request);

  @Operation(summary = "Buscar meta", description = "Com os 12 meses do ano pedido.")
  @ApiResponse(responseCode = "200", description = "Meta encontrada")
  @ApiResponse(responseCode = "404", description = "Meta não encontrada")
  ResponseEntity<MetaResponse> buscar(
      @PathVariable UUID id,
      @UsuarioLogado UUID usuarioId,
      @RequestParam(required = false) Integer ano);

  @Operation(summary = "Atualizar meta")
  @ApiResponse(responseCode = "200", description = "Meta atualizada")
  @ApiResponse(responseCode = "400", description = "Dados inválidos ou nome repetido")
  @ApiResponse(responseCode = "404", description = "Meta não encontrada")
  ResponseEntity<MetaResponse> atualizar(
      @PathVariable UUID id,
      @UsuarioLogado UUID usuarioId,
      @Valid @RequestBody AtualizarMetaRequest request);

  @Operation(summary = "Inativar meta", description = "A meta some do painel; o histórico fica.")
  @ApiResponse(responseCode = "204", description = "Meta inativada (idempotente)")
  @ApiResponse(responseCode = "404", description = "Meta não encontrada")
  ResponseEntity<Void> inativar(@PathVariable UUID id, @UsuarioLogado UUID usuarioId);

  @Operation(summary = "Ativar meta")
  @ApiResponse(responseCode = "200", description = "Meta ativada")
  @ApiResponse(responseCode = "404", description = "Meta não encontrada")
  ResponseEntity<MetaResponse> ativar(@PathVariable UUID id, @UsuarioLogado UUID usuarioId);

  @Operation(
      summary = "Registrar aporte e juros do mês",
      description =
          "Cria ou substitui o aporte e os juros/variação do mês. Zerar os dois zera o mês. "
              + "Aporte negativo é resgate.")
  @ApiResponse(responseCode = "200", description = "Mês salvo; devolve a meta com o ano")
  @ApiResponse(responseCode = "400", description = "Mês inválido ou meta inativa")
  @ApiResponse(responseCode = "404", description = "Meta não encontrada")
  ResponseEntity<MetaResponse> registrarMovimento(
      @PathVariable UUID id,
      @UsuarioLogado UUID usuarioId,
      @PathVariable int ano,
      @PathVariable int mes,
      @Valid @RequestBody MovimentoMetaRequest request);
}
