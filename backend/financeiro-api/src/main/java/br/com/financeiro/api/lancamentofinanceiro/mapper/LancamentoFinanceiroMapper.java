package br.com.financeiro.api.lancamentofinanceiro.mapper;

import br.com.financeiro.api.lancamentofinanceiro.dto.CriarLancamentoFinanceiroRequest;
import br.com.financeiro.api.lancamentofinanceiro.dto.LancamentoFinanceiroResponse;
import br.com.financeiro.api.lancamentofinanceiro.entity.LancamentoFinanceiro;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LancamentoFinanceiroMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "usuario", ignore = true)
  @Mapping(target = "conta", ignore = true)
  @Mapping(target = "categoria", ignore = true)
  @Mapping(target = "descricao", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "criadoEm", ignore = true)
  @Mapping(target = "atualizadoEm", ignore = true)
  LancamentoFinanceiro paraEntity(CriarLancamentoFinanceiroRequest request);

  @Mapping(target = "usuarioId", source = "usuario.id")
  @Mapping(target = "contaId", source = "conta.id")
  @Mapping(target = "nomeConta", source = "conta.nome")
  @Mapping(target = "categoriaId", source = "categoria.id")
  @Mapping(target = "nomeCategoria", source = "categoria.nome")
  LancamentoFinanceiroResponse paraResponse(LancamentoFinanceiro lancamentoFinanceiro);
}
