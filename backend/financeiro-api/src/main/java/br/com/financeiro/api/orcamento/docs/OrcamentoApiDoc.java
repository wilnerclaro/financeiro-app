package br.com.financeiro.api.orcamento.docs;

import br.com.financeiro.api.orcamento.dto.CopiaDePrevistoResponse;
import br.com.financeiro.api.orcamento.dto.CopiarPrevistoRequest;
import br.com.financeiro.api.orcamento.dto.DefinirOrcamentoRequest;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse;
import br.com.financeiro.api.orcamento.dto.RepetirPrevistoRequest;
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
      summary = "Copiar o previsto de um mês para outros",
      description =
          "Copia todas as categorias com previsto no mês da URL para os meses informados. Com "
              + "substituirExistentes=false, a categoria que já tem previsto no destino é "
              + "mantida. Cada mês continua editável sozinho depois da cópia.")
  @ApiResponse(responseCode = "200", description = "Quantas categorias foram gravadas e mantidas")
  @ApiResponse(
      responseCode = "400",
      description = "Mês de origem sem previsto, mês de origem entre os destinos ou mês inválido")
  ResponseEntity<CopiaDePrevistoResponse> copiar(
      @UsuarioLogado UUID usuarioId,
      @PathVariable int ano,
      @PathVariable int mes,
      @Valid @RequestBody CopiarPrevistoRequest request);

  @Operation(
      summary = "Repetir o previsto de uma categoria em vários meses",
      description =
          "Grava o mesmo valor (aluguel, luz, água) nos meses informados. Com "
              + "substituirExistentes=false, o mês que já tem previsto é mantido. Cada mês "
              + "continua editável sozinho depois.")
  @ApiResponse(responseCode = "200", description = "Quais meses foram gravados e quais mantidos")
  @ApiResponse(
      responseCode = "400",
      description = "Valor negativo, categoria com subcategorias, nenhum mês ou mês inválido")
  @ApiResponse(responseCode = "404", description = "Categoria não encontrada")
  ResponseEntity<CopiaDePrevistoResponse> repetir(
      @UsuarioLogado UUID usuarioId, @Valid @RequestBody RepetirPrevistoRequest request);

  @Operation(
      summary = "Resumo do ano",
      description =
          "Total e média do ano por categoria, previsto e realizado, com os 12 meses. A média "
              + "conta só os meses com valor.")
  @ApiResponse(responseCode = "200", description = "Resumo do ano")
  ResponseEntity<ResumoAnualOrcamentoResponse> resumoAnual(
      @UsuarioLogado UUID usuarioId, @PathVariable int ano);
}
