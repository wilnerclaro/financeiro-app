package br.com.financeiro.api.conta.mapper;

import br.com.financeiro.api.conta.dto.ContaResponse;
import br.com.financeiro.api.conta.dto.CriarContaRequest;
import br.com.financeiro.api.conta.entity.Conta;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContaMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "usuario", ignore = true)
  @Mapping(target = "nome", ignore = true)
  @Mapping(target = "saldoAtual", ignore = true)
  @Mapping(target = "ativa", ignore = true)
  @Mapping(target = "criadoEm", ignore = true)
  @Mapping(target = "atualizadoEm", ignore = true)
  Conta paraEntity(CriarContaRequest request);

  @Mapping(target = "usuarioId", source = "usuario.id")
  ContaResponse paraResponse(Conta conta);
}
