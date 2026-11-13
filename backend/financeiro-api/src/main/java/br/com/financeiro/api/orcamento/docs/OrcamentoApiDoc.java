package br.com.financeiro.api.orcamento.docs;

import br.com.financeiro.api.orcamento.dto.DefinirOrcamentoRequest;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse;
import br.com.financeiro.api.orcamento.dto.ResumoAnualOrcamentoResponse;
import br.com.financeiro.api.security.UsuarioLogado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(
    name = "Orçamento",
    description = "Previsto do mês por categoria e o relatório Previsto x Realizado")
public interface OrcamentoApiDoc {

  @Operation(
      summary = "Previsto x Realizado do mês",
      description =
          "Receitas (com totais de fonte ativa e passiva) e despesas por grupo e subitem. O "
              + "realizado é a soma dos lançamentos pagos pela competência. Percentuais são "
              + "fração (1.0 = 100%).")
  @ApiResponse(responseCode = "200", description = "Relatório do mês")
  @ApiResponse(responseCode = "400", description = "Ano ou mês inválido")
  ResponseEntity<PrevistoRealizadoResponse> previstoRealizado(
      @UsuarioLogado UUID usuarioId, @PathVariable int ano, @PathVariable int mes);

  @Operation(
      summary = "Definir o previsto do mês",
      description =
          "Cria ou substitui o previsto das categorias informadas. Só vale para categorias sem "
              + "subcategorias; o grupo soma os subitens.")
  @ApiResponse(responseCode = "200", description = "Previsto salvo; devolve o relatório do mês")
  @ApiResponse(
      responseCode = "400",
      description = "Valor negativo, categoria repetida ou categoria com subcategorias")
  @ApiResponse(responseCode = "404", description = "Categoria não encontrada")
  ResponseEntity<PrevistoRealizadoResponse> definir(
      @UsuarioLogado UUID usuarioId,
      @PathVariable int ano,
      @PathVariable int mes,
      @Valid @RequestBody DefinirOrcamentoRequest request);

  @Operation(
      summary = "Copiar o previsto do mês anterior",
      description = "Copia só para as categorias que ainda não têm previsto no mês.")
  @ApiResponse(responseCode = "200", description = "Previsto copiado; devolve o relatório do mês")
  @ApiResponse(responseCode = "400", description = "O mês anterior não tem previsto")
  ResponseEntity<PrevistoRealizadoResponse> copiarMesAnterior(
      @UsuarioLogado UUID usuarioId, @PathVariable int ano, @PathVariable int mes);

  @Operation(
      summary = "Resumo do ano",
      description =
          "Total e média do ano por categoria, previsto e realizado, com os 12 meses. A média "
              + "conta só os meses com valor.")
  @ApiResponse(responseCode = "200", description = "Resumo do ano")
  ResponseEntity<ResumoAnualOrcamentoResponse> resumoAnual(
      @UsuarioLogado UUID usuarioId, @PathVariable int ano);
}
