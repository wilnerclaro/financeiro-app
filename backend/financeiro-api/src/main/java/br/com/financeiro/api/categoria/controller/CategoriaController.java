package br.com.financeiro.api.categoria.controller;

import br.com.financeiro.api.categoria.dto.AtualizarCategoriaRequest;
import br.com.financeiro.api.categoria.dto.CategoriaResponse;
import br.com.financeiro.api.categoria.dto.CriarCategoriaRequest;
import br.com.financeiro.api.categoria.docs.CategoriaApiDoc;
import br.com.financeiro.api.categoria.enums.TipoCategoria;
import br.com.financeiro.api.categoria.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController implements CategoriaApiDoc {

    private final CategoriaService categoriaService;

    @PostMapping
    public ResponseEntity<CategoriaResponse> criar(
            @Valid @RequestBody CriarCategoriaRequest request
    ) {
        CategoriaResponse response = categoriaService.criar(request);
        URI location = URI.create("/api/categorias/" + response.id());

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<CategoriaResponse>> listar(
            @RequestParam UUID usuarioId,
            @RequestParam(required = false) TipoCategoria tipo,
            @RequestParam(required = false) Boolean ativa,
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        Page<CategoriaResponse> response = categoriaService.listar(
                usuarioId,
                tipo,
                ativa,
                pageable
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> buscarPorId(
            @PathVariable UUID id,
            @RequestParam UUID usuarioId
    ) {
        CategoriaResponse response = categoriaService.buscarPorId(id, usuarioId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarCategoriaRequest request
    ) {
        CategoriaResponse response = categoriaService.atualizar(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inativar(
            @PathVariable UUID id,
            @RequestParam UUID usuarioId
    ) {
        categoriaService.inativar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<CategoriaResponse> ativar(
            @PathVariable UUID id,
            @RequestParam UUID usuarioId
    ) {
        CategoriaResponse response = categoriaService.ativar(id, usuarioId);
        return ResponseEntity.ok(response);
    }
}
