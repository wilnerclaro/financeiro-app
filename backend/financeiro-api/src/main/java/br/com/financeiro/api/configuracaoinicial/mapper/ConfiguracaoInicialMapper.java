package br.com.financeiro.api.configuracaoinicial.mapper;

import br.com.financeiro.api.configuracaoinicial.dto.ConfiguracaoInicialResponse;
import br.com.financeiro.api.configuracaoinicial.entity.ConfiguracaoInicial;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConfiguracaoInicialMapper {

  ConfiguracaoInicialResponse paraResponse(ConfiguracaoInicial configuracao);
}
