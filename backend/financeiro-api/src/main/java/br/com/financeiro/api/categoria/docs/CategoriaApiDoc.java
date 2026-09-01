package br.com.financeiro.api.categoria.docs;

import br.com.financeiro.api.categoria.dto.AtualizarCategoriaRequest;
import br.com.financeiro.api.categoria.dto.CategoriaResponse;
import br.com.financeiro.api.categoria.dto.CriarCategoriaRequest;
import br.com.financeiro.api.categoria.enums.TipoCategoria;
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

@Tag(name = "Categorias", description = "Gerenciamento de categorias financeiras")
public interface CategoriaApiDoc {

  @Operation(
      summary = "Criar categoria",
      description = "Cria uma categoria financeira para um usuario.")
  @ApiResponse(responseCode = "201", description = "Categoria criada com sucesso")
  @ApiResponse(
      responseCode = "400",
      description = "Requisicao invalida ou regra de negocio violada")
  @ApiResponse(responseCode = "404", description = "Usuario ou categoria pai nao encontrada")
  ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CriarCategoriaRequest request);

  @Operation(
      summary = "Listar categorias",
      description = "Lista categorias por usuario com filtros opcionais.")
  @ApiResponse(responseCode = "200", description = "Categorias listadas com sucesso")
  @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
  ResponseEntity<Page<CategoriaResponse>> listar(
      @RequestParam UUID usuarioId,
      @RequestParam(required = false) TipoCategoria tipo,
      @RequestParam(required = false) Boolean ativa,
      Pageable pageable);

  @Operation(summary = "Buscar categoria", description = "Busca uma categoria pelo id e usuario.")
  @ApiResponse(responseCode = "200", description = "Categoria encontrada")
  @ApiResponse(responseCode = "404", description = "Categoria nao encontrada")
  ResponseEntity<CategoriaResponse> buscarPorId(
      @PathVariable UUID id, @RequestParam UUID usuarioId);

  @Operation(
      summary = "Atualizar categoria",
      description = "Atualiza dados, tipo, categoria pai e status ativo.")
  @ApiResponse(responseCode = "200", description = "Categoria atualizada com sucesso")
  @ApiResponse(
      responseCode = "400",
      description = "Requisicao invalida ou regra de negocio violada")
  @ApiResponse(responseCode = "404", description = "Categoria nao encontrada")
  ResponseEntity<CategoriaResponse> atualizar(
      @PathVariable UUID id, @Valid @RequestBody AtualizarCategoriaRequest request);

  @Operation(summary = "Inativar categoria", description = "Inativa uma categoria financeira.")
  @ApiResponse(responseCode = "204", description = "Categoria inativada com sucesso")
  @ApiResponse(responseCode = "400", description = "Categoria possui subcategorias ativas")
  @ApiResponse(responseCode = "404", description = "Categoria nao encontrada")
  ResponseEntity<Void> inativar(@PathVariable UUID id, @RequestParam UUID usuarioId);

  @Operation(summary = "Ativar categoria", description = "Reativa uma categoria financeira.")
  @ApiResponse(responseCode = "200", description = "Categoria ativada com sucesso")
  @ApiResponse(responseCode = "404", description = "Categoria nao encontrada")
  ResponseEntity<CategoriaResponse> ativar(@PathVariable UUID id, @RequestParam UUID usuarioId);
}
