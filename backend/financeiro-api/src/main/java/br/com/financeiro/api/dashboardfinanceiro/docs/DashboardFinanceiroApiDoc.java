package br.com.financeiro.api.dashboardfinanceiro.docs;

import br.com.financeiro.api.dashboardfinanceiro.dto.InicioResponse;
import br.com.financeiro.api.dashboardfinanceiro.dto.ResumoMensalFinanceiroResponse;
import br.com.financeiro.api.security.UsuarioLogado;
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
          "Retorna receitas, despesas, saldo do período e distribuição por categoria para um mês.")
  @ApiResponse(responseCode = "200", description = "Resumo mensal retornado com sucesso")
  @ApiResponse(responseCode = "400", description = "Ano ou mês inválido")
  @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
  ResponseEntity<ResumoMensalFinanceiroResponse> buscarResumoMensal(
      @UsuarioLogado UUID usuarioId, @RequestParam Integer ano, @RequestParam Integer mes);

  @Operation(
      summary = "Tela Início",
      description =
          "Entrou, saiu e sobrou do mês; metas de 30% e 50%; uso do orçamento; "
              + "Independenciômetro com faixa, próximo marco e quanto falta; evolução de 6 meses; "
              + "próximos vencimentos; categorias acima do previsto e saldo em contas. Sem ano e "
              + "mês, usa o mês atual.")
  @ApiResponse(responseCode = "200", description = "Dados da tela Início")
  @ApiResponse(responseCode = "400", description = "Ano ou mês inválido, ou só um dos dois")
  ResponseEntity<InicioResponse> inicio(
      @UsuarioLogado UUID usuarioId,
      @RequestParam(required = false) Integer ano,
      @RequestParam(required = false) Integer mes);
}
