package br.com.financeiro.api.transferencia.mapper;

import br.com.financeiro.api.transferencia.dto.TransferenciaResponse;
import br.com.financeiro.api.transferencia.entity.Transferencia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransferenciaMapper {

  @Mapping(target = "contaOrigemId", source = "contaOrigem.id")
  @Mapping(target = "nomeContaOrigem", source = "contaOrigem.nome")
  @Mapping(target = "contaDestinoId", source = "contaDestino.id")
  @Mapping(target = "nomeContaDestino", source = "contaDestino.nome")
  TransferenciaResponse paraResponse(Transferencia transferencia);
}
