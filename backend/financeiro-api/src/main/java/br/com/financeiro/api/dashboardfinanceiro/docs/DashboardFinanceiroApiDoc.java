package br.com.financeiro.api.dashboardfinanceiro.docs;

import br.com.financeiro.api.dashboardfinanceiro.dto.ResumoMensalFinanceiroResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Dashboard Financeiro", description = "Indicadores e resumos do controle financeiro")
public interface DashboardFinanceiroApiDoc {

  @Operation(
      summary = "Buscar resumo mensal",
      description =
          "Retorna receitas, despesas, saldo do periodo e distribuicao por categoria para um mes.")
  @ApiResponse(responseCode = "200", description = "Resumo mensal retornado com sucesso")
  @ApiResponse(responseCode = "400", description = "Ano ou mes invalido")
  @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
  ResponseEntity<ResumoMensalFinanceiroResponse> buscarResumoMensal(
      @RequestParam UUID usuarioId, @RequestParam Integer ano, @RequestParam Integer mes);
}
