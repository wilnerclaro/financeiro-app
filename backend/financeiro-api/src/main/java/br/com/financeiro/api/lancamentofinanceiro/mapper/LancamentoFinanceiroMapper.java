package br.com.financeiro.api.lancamentofinanceiro.mapper;

import br.com.financeiro.api.lancamentofinanceiro.dto.LancamentoFinanceiroResponse;
import br.com.financeiro.api.lancamentofinanceiro.entity.LancamentoFinanceiro;
import java.time.LocalDate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LancamentoFinanceiroMapper {

  @Mapping(target = "usuarioId", source = "usuario.id")
  @Mapping(target = "contaId", source = "conta.id")
  @Mapping(target = "nomeConta", source = "conta.nome")
  @Mapping(target = "categoriaId", source = "categoria.id")
  @Mapping(target = "nomeCategoria", source = "categoria.nome")
  LancamentoFinanceiroResponse paraResponse(LancamentoFinanceiro lancamentoFinanceiro);

  default LancamentoFinanceiroResponse paraResponse(
      LancamentoFinanceiro lancamentoFinanceiro, LocalDate hoje) {
    return paraResponse(lancamentoFinanceiro).comStatus(lancamentoFinanceiro.statusEm(hoje));
  }
}
