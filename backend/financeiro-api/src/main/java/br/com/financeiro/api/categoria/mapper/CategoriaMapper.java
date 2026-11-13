package br.com.financeiro.api.categoria.mapper;

import br.com.financeiro.api.categoria.dto.CategoriaResponse;
import br.com.financeiro.api.categoria.dto.CriarCategoriaRequest;
import br.com.financeiro.api.categoria.entity.Categoria;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

  @Mapping(target = "usuarioId", source = "usuario.id")
  @Mapping(target = "categoriaPaiId", source = "categoriaPai.id")
  @Mapping(target = "nomeCategoriaPai", source = "categoriaPai.nome")
  CategoriaResponse paraResponse(Categoria categoria);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "usuario", ignore = true)
  @Mapping(target = "categoriaPai", ignore = true)
  @Mapping(target = "ativa", ignore = true)
  @Mapping(target = "criadoEm", ignore = true)
  @Mapping(target = "atualizadoEm", ignore = true)
  Categoria paraEntity(CriarCategoriaRequest request);
}
