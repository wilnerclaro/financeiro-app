package br.com.financeiro.api.configuracaoinicial.service;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.util.Competencia;
import br.com.financeiro.api.configuracaoinicial.dto.AtualizarConfiguracaoInicialRequest;
import br.com.financeiro.api.configuracaoinicial.dto.ConfiguracaoInicialResponse;
import br.com.financeiro.api.configuracaoinicial.entity.ConfiguracaoInicial;
import br.com.financeiro.api.configuracaoinicial.enums.EtapaConfiguracao;
import br.com.financeiro.api.configuracaoinicial.mapper.ConfiguracaoInicialMapper;
import br.com.financeiro.api.configuracaoinicial.repository.ConfiguracaoInicialRepository;
import br.com.financeiro.api.usuario.service.UsuarioService;
import java.time.Clock;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfiguracaoInicialService {

  private final ConfiguracaoInicialRepository repository;
  private final ConfiguracaoInicialMapper mapper;
  private final UsuarioService usuarioService;
  private final Clock clock;

  @Transactional(readOnly = true)
  public ConfiguracaoInicialResponse buscar(UUID usuarioId) {
    usuarioService.buscar(usuarioId);
    return repository
        .findByUsuarioId(usuarioId)
        .map(mapper::paraResponse)
        .orElseGet(
            () -> {
              YearMonth competencia = YearMonth.now(clock);
              return new ConfiguracaoInicialResponse(
                  null,
                  EtapaConfiguracao.BOAS_VINDAS,
                  false,
                  competencia.getYear(),
                  competencia.getMonthValue());
            });
  }

  @Transactional
  public ConfiguracaoInicialResponse atualizar(
      UUID usuarioId, AtualizarConfiguracaoInicialRequest request) {
    Competencia.de(request.ano(), request.mes());
    if (request.rendaPassivaDesejada() == null
        && !EnumSet.of(EtapaConfiguracao.BOAS_VINDAS, EtapaConfiguracao.OBJETIVO)
            .contains(request.etapa())) {
      throw new BusinessException("Defina seu objetivo de renda passiva antes de continuar.");
    }
    ConfiguracaoInicial configuracao =
        repository
            .findByUsuarioId(usuarioId)
            .orElseGet(
                () ->
                    ConfiguracaoInicial.builder()
                        .usuario(usuarioService.buscar(usuarioId))
                        .build());
    configuracao.setRendaPassivaDesejada(request.rendaPassivaDesejada());
    configuracao.setEtapa(request.etapa());
    configuracao.setPausado(request.pausado());
    configuracao.setAno(request.ano());
    configuracao.setMes(request.mes());
    return mapper.paraResponse(repository.save(configuracao));
  }
}
