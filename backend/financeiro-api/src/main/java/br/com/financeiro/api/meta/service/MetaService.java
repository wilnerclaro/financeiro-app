package br.com.financeiro.api.meta.service;

import static br.com.financeiro.api.common.util.Competencia.MESES_NO_ANO;
import static br.com.financeiro.api.common.util.Dinheiro.ZERO;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.exception.ResourceNotFoundException;
import br.com.financeiro.api.common.util.Competencia;
import br.com.financeiro.api.common.util.Dinheiro;
import br.com.financeiro.api.common.util.Fracao;
import br.com.financeiro.api.common.util.NormalizadorTexto;
import br.com.financeiro.api.meta.dto.AtualizarMetaRequest;
import br.com.financeiro.api.meta.dto.CriarMetaRequest;
import br.com.financeiro.api.meta.dto.MetaResponse;
import br.com.financeiro.api.meta.dto.MetaResponse.MesDaMeta;
import br.com.financeiro.api.meta.dto.MovimentoMetaRequest;
import br.com.financeiro.api.meta.dto.PainelMetasResponse;
import br.com.financeiro.api.meta.entity.Meta;
import br.com.financeiro.api.meta.entity.MovimentoMeta;
import br.com.financeiro.api.meta.repository.MetaRepository;
import br.com.financeiro.api.meta.repository.MovimentoMetaRepository;
import br.com.financeiro.api.usuario.service.UsuarioService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MetaService {

  private final MetaRepository metaRepository;
  private final MovimentoMetaRepository movimentoMetaRepository;
  private final UsuarioService usuarioService;
  private final Clock clock;

  @Transactional
  public MetaResponse criar(UUID usuarioId, CriarMetaRequest request) {
    String nome = NormalizadorTexto.normalizarNome(request.nome());
    validarNomeUnico(usuarioId, nome, null);

    Meta meta =
        Meta.builder()
            .usuario(usuarioService.referencia(usuarioId))
            .nome(nome)
            .valorNecessario(Dinheiro.arredondar(request.valorNecessario()))
            .investimentoInicial(Dinheiro.arredondar(request.investimentoInicial()))
            .ativa(true)
            .build();
    meta = metaRepository.saveAndFlush(meta);
    return paraResponse(meta, List.of(), anoAtual());
  }

  @Transactional
  public MetaResponse atualizar(UUID id, UUID usuarioId, AtualizarMetaRequest request) {
    Meta meta = buscarDoUsuario(id, usuarioId);
    String nome = NormalizadorTexto.normalizarNome(request.nome());
    validarNomeUnico(usuarioId, nome, id);

    meta.setNome(nome);
    meta.setValorNecessario(Dinheiro.arredondar(request.valorNecessario()));
    meta.setInvestimentoInicial(Dinheiro.arredondar(request.investimentoInicial()));
    metaRepository.flush();
    return buscar(id, usuarioId, anoAtual());
  }

  @Transactional
  public void inativar(UUID id, UUID usuarioId) {
    Meta meta = buscarDoUsuario(id, usuarioId);
    meta.setAtiva(false);
  }

  @Transactional
  public MetaResponse ativar(UUID id, UUID usuarioId) {
    Meta meta = buscarDoUsuario(id, usuarioId);
    meta.setAtiva(true);
    metaRepository.flush();
    return buscar(id, usuarioId, anoAtual());
  }

  @Transactional
  public MetaResponse registrarMovimento(
      UUID id, UUID usuarioId, int ano, int mes, MovimentoMetaRequest request) {
    Competencia.de(ano, mes);
    Meta meta = buscarDoUsuario(id, usuarioId);
    if (!Boolean.TRUE.equals(meta.getAtiva())) {
      throw new BusinessException("Ative a meta antes de registrar aportes.");
    }

    BigDecimal aporte = Dinheiro.arredondar(request.aporte());
    BigDecimal juros = Dinheiro.arredondar(request.juros());
    MovimentoMeta movimento =
        movimentoMetaRepository
            .buscarDoMes(id, ano, mes)
            .orElseGet(() -> MovimentoMeta.builder().meta(meta).ano(ano).mes(mes).build());
    movimento.setAporte(aporte);
    movimento.setJuros(juros);
    movimentoMetaRepository.saveAndFlush(movimento);
    return buscar(id, usuarioId, ano);
  }

  @Transactional(readOnly = true)
  public MetaResponse buscar(UUID id, UUID usuarioId, Integer ano) {
    Meta meta = buscarDoUsuario(id, usuarioId);
    int anoDoPainel = ano == null ? anoAtual() : Competencia.validarAno(ano);
    return paraResponse(
        meta, movimentoMetaRepository.buscarDasMetas(List.of(meta.getId())), anoDoPainel);
  }

  @Transactional(readOnly = true)
  public PainelMetasResponse painel(UUID usuarioId, Integer ano, boolean incluirInativas) {
    int anoDoPainel = ano == null ? anoAtual() : Competencia.validarAno(ano);
    List<Meta> metas = metaRepository.buscarDoUsuario(usuarioId, incluirInativas);

    Map<UUID, List<MovimentoMeta>> movimentosPorMeta =
        metas.isEmpty()
            ? Map.of()
            : movimentoMetaRepository
                .buscarDasMetas(metas.stream().map(Meta::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(m -> m.getMeta().getId()));

    List<MetaResponse> respostas =
        metas.stream()
            .map(
                m ->
                    paraResponse(
                        m, movimentosPorMeta.getOrDefault(m.getId(), List.of()), anoDoPainel))
            .toList();

    BigDecimal totalNecessario = ZERO;
    BigDecimal totalAcumulado = ZERO;
    BigDecimal[] aportes = new BigDecimal[MESES_NO_ANO];
    BigDecimal[] juros = new BigDecimal[MESES_NO_ANO];
    for (int i = 0; i < MESES_NO_ANO; i++) {
      aportes[i] = ZERO;
      juros[i] = ZERO;
    }
    for (MetaResponse meta : respostas) {
      totalNecessario = totalNecessario.add(meta.valorNecessario());
      totalAcumulado = totalAcumulado.add(meta.acumulado());
      for (MesDaMeta mes : meta.meses()) {
        aportes[mes.mes() - 1] = aportes[mes.mes() - 1].add(mes.aporte());
        juros[mes.mes() - 1] = juros[mes.mes() - 1].add(mes.juros());
      }
    }
    List<MesDaMeta> totaisPorMes = new ArrayList<>();
    for (int i = 0; i < MESES_NO_ANO; i++) {
      totaisPorMes.add(new MesDaMeta(i + 1, aportes[i], juros[i]));
    }

    return new PainelMetasResponse(
        anoDoPainel,
        totalNecessario,
        totalAcumulado,
        Fracao.de(totalAcumulado, totalNecessario),
        respostas,
        totaisPorMes);
  }

  static MetaResponse paraResponse(Meta meta, List<MovimentoMeta> movimentos, int ano) {
    BigDecimal acumulado = meta.getInvestimentoInicial();
    BigDecimal[] aportes = new BigDecimal[MESES_NO_ANO];
    BigDecimal[] juros = new BigDecimal[MESES_NO_ANO];
    for (MovimentoMeta movimento : movimentos) {
      acumulado = acumulado.add(movimento.getAporte()).add(movimento.getJuros());
      if (movimento.getAno() == ano) {
        aportes[movimento.getMes() - 1] = movimento.getAporte();
        juros[movimento.getMes() - 1] = movimento.getJuros();
      }
    }

    List<MesDaMeta> meses = new ArrayList<>();
    for (int i = 0; i < MESES_NO_ANO; i++) {
      meses.add(
          new MesDaMeta(i + 1, Dinheiro.arredondar(aportes[i]), Dinheiro.arredondar(juros[i])));
    }

    acumulado = Dinheiro.arredondar(acumulado);
    BigDecimal falta = meta.getValorNecessario().subtract(acumulado).max(ZERO);
    return new MetaResponse(
        meta.getId(),
        meta.getNome(),
        meta.getValorNecessario(),
        meta.getInvestimentoInicial(),
        Boolean.TRUE.equals(meta.getAtiva()),
        acumulado,
        Fracao.de(acumulado, meta.getValorNecessario()),
        falta,
        acumulado.compareTo(meta.getValorNecessario()) >= 0,
        meses);
  }

  private Meta buscarDoUsuario(UUID id, UUID usuarioId) {
    return metaRepository
        .buscarPorIdEUsuarioId(id, usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Meta não encontrada."));
  }

  private void validarNomeUnico(UUID usuarioId, String nome, UUID idIgnorado) {
    if (metaRepository.existeComNome(usuarioId, nome, idIgnorado)) {
      throw new BusinessException("Já existe uma meta com esse nome.");
    }
  }

  private int anoAtual() {
    return LocalDate.now(clock).getYear();
  }
}
