package br.com.financeiro.api.dashboardfinanceiro.controller;

import br.com.financeiro.api.dashboardfinanceiro.docs.DashboardFinanceiroApiDoc;
import br.com.financeiro.api.dashboardfinanceiro.dto.ResumoMensalFinanceiroResponse;
import br.com.financeiro.api.dashboardfinanceiro.service.DashboardFinanceiroService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard-financeiro")
@RequiredArgsConstructor
public class DashboardFinanceiroController implements DashboardFinanceiroApiDoc {

  private final DashboardFinanceiroService dashboardFinanceiroService;

  @GetMapping("/resumo-mensal")
  public ResponseEntity<ResumoMensalFinanceiroResponse> buscarResumoMensal(
      @RequestParam UUID usuarioId, @RequestParam Integer ano, @RequestParam Integer mes) {
    ResumoMensalFinanceiroResponse response =
        dashboardFinanceiroService.buscarResumoMensal(usuarioId, ano, mes);

    return ResponseEntity.ok(response);
  }
}
