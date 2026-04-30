package br.com.financeiro.api.conta.controller;

import br.com.financeiro.api.conta.dto.ContaResponse;
import br.com.financeiro.api.conta.dto.CriarContaRequest;
import br.com.financeiro.api.conta.service.ContaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "Contas", description = "Gerenciamento de contas financeiras")
@RestController
@RequestMapping("/api/contas")
@RequiredArgsConstructor
public class ContaController {

    private final ContaService contaService;

    @PostMapping
    public ResponseEntity<ContaResponse> criar(
            @Valid @RequestBody CriarContaRequest request
    ) {
        ContaResponse response = contaService.criar(request);
        URI location = URI.create("/api/contas/" + response.id());

        return ResponseEntity.created(location).body(response);
    }
}