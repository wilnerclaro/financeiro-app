package br.com.financeiro.api.categoria.dto;

import br.com.financeiro.api.categoria.enums.TipoCategoria;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CategoriaResponse(
    UUID id,
    UUID usuarioId,
    UUID categoriaPaiId,
    String nomeCategoriaPai,
    String nome,
    TipoCategoria tipo,
    Boolean ativa,
    OffsetDateTime criadoEm,
    OffsetDateTime atualizadoEm) {}
