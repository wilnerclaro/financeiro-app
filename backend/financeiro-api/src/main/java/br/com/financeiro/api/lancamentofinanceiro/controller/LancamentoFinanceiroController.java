package br.com.financeiro.api.lancamentofinanceiro.controller;

import br.com.financeiro.api.lancamentofinanceiro.dto.AtualizarLancamentoFinanceiroRequest;
import br.com.financeiro.api.lancamentofinanceiro.dto.CriarLancamentoFinanceiroRequest;
import br.com.financeiro.api.lancamentofinanceiro.dto.LancamentoFinanceiroResponse;
import br.com.financeiro.api.lancamentofinanceiro.dto.PagarLancamentoFinanceiroRequest;
import br.com.financeiro.api.lancamentofinanceiro.enums.StatusLancamentoFinanceiro;
import br.com.financeiro.api.lancamentofinanceiro.enums.TipoLancamentoFinanceiro;
import br.com.financeiro.api.lancamentofinanceiro.service.LancamentoFinanceiroService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@Tag(name = "Lancamentos Financeiros", description = "Gerenciamento de receitas e despesas")
@RestController
@RequestMapping("/api/lancamentos-financeiros")
@RequiredArgsConstructor
public class LancamentoFinanceiroController {

    private final LancamentoFinanceiroService lancamentoFinanceiroService;

    @PostMapping
    public ResponseEntity<LancamentoFinanceiroResponse> criar(
            @Valid @RequestBody CriarLancamentoFinanceiroRequest request
    ) {
        LancamentoFinanceiroResponse response = lancamentoFinanceiroService.criar(request);
        URI location = URI.create("/api/lancamentos-financeiros/" + response.id());

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<LancamentoFinanceiroResponse>> listar(
            @RequestParam UUID usuarioId,
            @RequestParam(required = false) TipoLancamentoFinanceiro tipo,
            @RequestParam(required = false) StatusLancamentoFinanceiro status,
            @RequestParam(required = false) UUID contaId,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @PageableDefault(size = 20, sort = "dataCompetencia", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<LancamentoFinanceiroResponse> response = lancamentoFinanceiroService.listar(
                usuarioId,
                tipo,
                status,
                contaId,
                categoriaId,
                dataInicio,
                dataFim,
                pageable
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LancamentoFinanceiroResponse> buscarPorId(
            @PathVariable UUID id,
            @RequestParam UUID usuarioId
    ) {
        LancamentoFinanceiroResponse response = lancamentoFinanceiroService.buscarPorId(id, usuarioId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LancamentoFinanceiroResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarLancamentoFinanceiroRequest request
    ) {
        LancamentoFinanceiroResponse response = lancamentoFinanceiroService.atualizar(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/pagar")
    public ResponseEntity<LancamentoFinanceiroResponse> pagar(
            @PathVariable UUID id,
            @Valid @RequestBody PagarLancamentoFinanceiroRequest request
    ) {
        LancamentoFinanceiroResponse response = lancamentoFinanceiroService.pagar(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(
            @PathVariable UUID id,
            @RequestParam UUID usuarioId
    ) {
        lancamentoFinanceiroService.cancelar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
