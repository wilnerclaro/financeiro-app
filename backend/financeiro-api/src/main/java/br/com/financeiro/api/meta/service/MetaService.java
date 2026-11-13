package br.com.financeiro.api.meta.service;

import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.exception.ResourceNotFoundException;
import br.com.financeiro.api.common.util.Fracao;
import br.com.financeiro.api.common.util.NormalizadorTexto;
import br.com.financeiro.api.meta.dto.MetaResponse;
import br.com.financeiro.api.meta.dto.MetaResponse.MesDaMeta;
import br.com.financeiro.api.meta.dto.MovimentoMetaRequest;
import br.com.financeiro.api.meta.dto.PainelMetasResponse;
import br.com.financeiro.api.meta.dto.SalvarMetaRequest;
import br.com.financeiro.api.meta.entity.Meta;
import br.com.financeiro.api.meta.entity.MovimentoMeta;
import br.com.financeiro.api.meta.repository.MetaRepository;
import br.com.financeiro.api.meta.repository.MovimentoMetaRepository;
import br.com.financeiro.api.usuario.service.UsuarioService;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

  private final MetaRepository metaRepository;
  private final MovimentoMetaRepository movimentoMetaRepository;
  private final UsuarioService usuarioService;
  private final Clock clock;

  @Transactional
  public MetaResponse criar(UUID usuarioId, SalvarMetaRequest request) {
    String nome = NormalizadorTexto.normalizarNome(request.nome());
    validarNomeUnico(usuarioId, nome, null);

    Meta meta =
        Meta.builder()
            .usuario(usuarioService.referencia(usuarioId))
            .nome(nome)
            .valorNecessario(escala2(request.valorNecessario()))
            .investimentoInicial(escala2(request.investimentoInicial()))
            .ativa(true)
            .build();
    meta = metaRepository.saveAndFlush(meta);
    return paraResponse(meta, List.of(), anoAtual());
  }

  @Transactional
  public MetaResponse atualizar(UUID id, UUID usuarioId, SalvarMetaRequest request) {
    Meta meta = buscarDoUsuario(id, usuarioId);
    String nome = NormalizadorTexto.normalizarNome(request.nome());
    validarNomeUnico(usuarioId, nome, id);

    meta.setNome(nome);
    meta.setValorNecessario(escala2(request.valorNecessario()));
    meta.setInvestimentoInicial(escala2(request.investimentoInicial()));
    metaRepository.flush();
    return buscar(id, usuarioId, anoAtual());
  }

  @Transactional
  public void inativar(UUID id, UUID usuarioId) {
    Meta meta = buscarDoUsuario(id, usuarioId);
    if (!Boolean.TRUE.equals(meta.getAtiva())) {
      throw new BusinessException("A meta já está inativa.");
    }
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
    validarCompetencia(ano, mes);
    Meta meta = buscarDoUsuario(id, usuarioId);
    if (!Boolean.TRUE.equals(meta.getAtiva())) {
      throw new BusinessException("Ative a meta antes de registrar aportes.");
    }

    BigDecimal aporte = escala2(request.aporte());
    BigDecimal juros = escala2(request.juros());
    MovimentoMeta movimento = movimentoMetaRepository.buscarDoMes(id, ano, mes).orElse(null);

    if (aporte.signum() == 0 && juros.signum() == 0) {
      if (movimento != null) {
        movimentoMetaRepository.delete(movimento);
      }
    } else {
      if (movimento == null) {
        movimento = MovimentoMeta.builder().meta(meta).ano(ano).mes(mes).build();
      }
      movimento.setAporte(aporte);
      movimento.setJuros(juros);
      movimentoMetaRepository.save(movimento);
    }
    movimentoMetaRepository.flush();
    return buscar(id, usuarioId, ano);
  }

  @Transactional(readOnly = true)
  public MetaResponse buscar(UUID id, UUID usuarioId, Integer ano) {
    Meta meta = buscarDoUsuario(id, usuarioId);
    int anoDoPainel = ano == null ? anoAtual() : validarAno(ano);
    return paraResponse(
        meta, movimentoMetaRepository.buscarDasMetas(List.of(meta.getId())), anoDoPainel);
  }

  @Transactional(readOnly = true)
  public PainelMetasResponse painel(UUID usuarioId, Integer ano, boolean incluirInativas) {
    int anoDoPainel = ano == null ? anoAtual() : validarAno(ano);
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
    BigDecimal[] aportes = new BigDecimal[12];
    BigDecimal[] juros = new BigDecimal[12];
    for (int i = 0; i < 12; i++) {
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
    for (int i = 0; i < 12; i++) {
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
    BigDecimal[] aportes = new BigDecimal[12];
    BigDecimal[] juros = new BigDecimal[12];
    for (MovimentoMeta movimento : movimentos) {
      acumulado = acumulado.add(movimento.getAporte()).add(movimento.getJuros());
      if (movimento.getAno() == ano) {
        aportes[movimento.getMes() - 1] = movimento.getAporte();
        juros[movimento.getMes() - 1] = movimento.getJuros();
      }
    }

    List<MesDaMeta> meses = new ArrayList<>();
    for (int i = 0; i < 12; i++) {
      meses.add(new MesDaMeta(i + 1, escala2(aportes[i]), escala2(juros[i])));
    }

    acumulado = escala2(acumulado);
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

  private static int validarAno(int ano) {
    if (ano < 2000 || ano > 2100) {
      throw new BusinessException("O ano deve estar entre 2000 e 2100.");
    }
    return ano;
  }

  private static void validarCompetencia(int ano, int mes) {
    validarAno(ano);
    if (mes < 1 || mes > 12) {
      throw new BusinessException("O mês deve estar entre 1 e 12.");
    }
  }

  private static BigDecimal escala2(BigDecimal valor) {
    return valor == null ? ZERO : valor.setScale(2, RoundingMode.HALF_UP);
  }
}
