package br.com.financeiro.api.orcamento.service;

import static br.com.financeiro.api.common.util.Competencia.MESES_NO_ANO;
import static br.com.financeiro.api.common.util.Dinheiro.ZERO;

import br.com.financeiro.api.categoria.entity.Categoria;
import br.com.financeiro.api.categoria.enums.FonteReceita;
import br.com.financeiro.api.categoria.enums.TipoCategoria;
import br.com.financeiro.api.categoria.service.CategoriaService;
import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.exception.ResourceNotFoundException;
import br.com.financeiro.api.common.util.Competencia;
import br.com.financeiro.api.common.util.Dinheiro;
import br.com.financeiro.api.common.util.Fracao;
import br.com.financeiro.api.orcamento.dto.CopiaDePrevistoResponse;
import br.com.financeiro.api.orcamento.dto.CopiaDePrevistoResponse.ResultadoDoMes;
import br.com.financeiro.api.orcamento.dto.CopiarPrevistoRequest;
import br.com.financeiro.api.orcamento.dto.DefinirOrcamentoRequest;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse.Bloco;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse.Grupo;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse.Item;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse.Valores;
import br.com.financeiro.api.orcamento.dto.RepetirPrevistoRequest;
import br.com.financeiro.api.orcamento.dto.ResumoAnualOrcamentoResponse;
import br.com.financeiro.api.orcamento.entity.OrcamentoMensal;
import br.com.financeiro.api.orcamento.repository.OrcamentoMensalRepository;
import br.com.financeiro.api.orcamento.repository.OrcamentoMensalRepository.ValorPorCategoriaEMes;
import br.com.financeiro.api.usuario.service.UsuarioService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrcamentoService {

  private final OrcamentoMensalRepository orcamentoMensalRepository;
  private final CategoriaService categoriaService;
  private final UsuarioService usuarioService;

  @Transactional
  public PrevistoRealizadoResponse definir(
      UUID usuarioId, int ano, int mes, DefinirOrcamentoRequest request) {
    YearMonth competencia = Competencia.de(ano, mes);

    List<Categoria> categorias = categoriaService.listarTodasDoUsuario(usuarioId);
    Map<UUID, Categoria> porId = porId(categorias);
    Set<UUID> comSubcategorias = idsDosPais(categorias);

    Set<UUID> informadas = new HashSet<>();
    for (DefinirOrcamentoRequest.Item item : request.itens()) {
      if (!informadas.add(item.categoriaId())) {
        throw new BusinessException("A mesma categoria foi informada mais de uma vez.");
      }
      categoriaParaPrevisto(item.categoriaId(), porId, comSubcategorias);
    }

    Map<UUID, OrcamentoMensal> existentes = orcamentosDoMesPorCategoria(usuarioId, competencia);
    for (DefinirOrcamentoRequest.Item item : request.itens()) {
      gravar(
          usuarioId,
          porId.get(item.categoriaId()),
          competencia,
          Dinheiro.arredondar(item.valorPrevisto()),
          existentes,
          true);
    }
    orcamentoMensalRepository.flush();

    return montarRelatorio(usuarioId, ano, mes, categorias);
  }

  @Transactional
  public CopiaDePrevistoResponse repetir(UUID usuarioId, RepetirPrevistoRequest request) {
    List<Categoria> categorias = categoriaService.listarTodasDoUsuario(usuarioId);
    Categoria categoria =
        categoriaParaPrevisto(request.categoriaId(), porId(categorias), idsDosPais(categorias));
    BigDecimal valor = Dinheiro.arredondar(request.valorPrevisto());

    List<ResultadoDoMes> resultados = new ArrayList<>();
    for (YearMonth destino : destinos(request.meses())) {
      boolean gravou =
          gravar(
              usuarioId,
              categoria,
              destino,
              valor,
              orcamentosDoMesPorCategoria(usuarioId, destino),
              request.substituirExistentes());
      resultados.add(new ResultadoDoMes(destino, gravou ? 1 : 0, gravou ? 0 : 1));
    }
    orcamentoMensalRepository.flush();
    return new CopiaDePrevistoResponse(resultados);
  }

  @Transactional
  public CopiaDePrevistoResponse copiar(
      UUID usuarioId, int ano, int mes, CopiarPrevistoRequest request) {
    YearMonth origem = Competencia.de(ano, mes);
    SortedSet<YearMonth> destinos = destinos(request.meses());
    if (destinos.contains(origem)) {
      throw new BusinessException("O mês de origem não pode estar entre os destinos.");
    }
    List<OrcamentoMensal> doMes = orcamentoMensalRepository.buscarDoMes(usuarioId, ano, mes);
    if (doMes.isEmpty()) {
      throw new BusinessException("O mês de origem não tem previsto para copiar.");
    }

    List<ResultadoDoMes> resultados = new ArrayList<>();
    for (YearMonth destino : destinos) {
      Map<UUID, OrcamentoMensal> existentes = orcamentosDoMesPorCategoria(usuarioId, destino);
      int gravadas = 0;
      for (OrcamentoMensal previsto : doMes) {
        if (gravar(
            usuarioId,
            previsto.getCategoria(),
            destino,
            previsto.getValorPrevisto(),
            existentes,
            request.substituirExistentes())) {
          gravadas++;
        }
      }
      resultados.add(new ResultadoDoMes(destino, gravadas, doMes.size() - gravadas));
    }
    orcamentoMensalRepository.flush();
    return new CopiaDePrevistoResponse(resultados);
  }

  @Transactional(readOnly = true)
  public PrevistoRealizadoResponse previstoRealizado(UUID usuarioId, int ano, int mes) {
    Competencia.de(ano, mes);
    return montarRelatorio(usuarioId, ano, mes, categoriaService.listarTodasDoUsuario(usuarioId));
  }

  @Transactional(readOnly = true)
  public ResumoAnualOrcamentoResponse resumoAnual(UUID usuarioId, int ano) {
    Competencia.validarAno(ano);

    List<Categoria> categorias = categoriaService.listarTodasDoUsuario(usuarioId);
    Map<UUID, BigDecimal[]> previstos =
        porCategoriaEMes(orcamentoMensalRepository.buscarPrevistosDoAno(usuarioId, ano));
    Map<UUID, BigDecimal[]> realizados =
        porCategoriaEMes(
            orcamentoMensalRepository.buscarRealizados(
                usuarioId,
                Year.of(ano).atDay(1),
                Year.of(ano).atMonth(Month.DECEMBER).atEndOfMonth()));

    List<ResumoAnualOrcamentoResponse.Linha> linhas = new ArrayList<>();
    for (Categoria categoria : ordenarComoArvore(categorias)) {
      BigDecimal[] previsto =
          previstos.getOrDefault(categoria.getId(), new BigDecimal[MESES_NO_ANO]);
      BigDecimal[] realizado =
          realizados.getOrDefault(categoria.getId(), new BigDecimal[MESES_NO_ANO]);
      boolean temValor =
          previstos.containsKey(categoria.getId()) || realizados.containsKey(categoria.getId());
      if (!Boolean.TRUE.equals(categoria.getAtiva()) && !temValor) {
        continue;
      }

      List<ResumoAnualOrcamentoResponse.Mes> meses = new ArrayList<>();
      for (int i = 0; i < MESES_NO_ANO; i++) {
        meses.add(
            new ResumoAnualOrcamentoResponse.Mes(
                i + 1, Dinheiro.arredondar(previsto[i]), Dinheiro.arredondar(realizado[i])));
      }
      linhas.add(
          new ResumoAnualOrcamentoResponse.Linha(
              categoria.getId(),
              categoria.getNome(),
              categoria.getCategoriaPai() == null ? null : categoria.getCategoriaPai().getId(),
              categoria.getTipo(),
              somar(previsto),
              somar(realizado),
              mediaDosMesesComValor(previsto),
              mediaDosMesesComValor(realizado),
              meses));
    }
    return new ResumoAnualOrcamentoResponse(ano, linhas);
  }

  private PrevistoRealizadoResponse montarRelatorio(
      UUID usuarioId, int ano, int mes, List<Categoria> categorias) {
    YearMonth competencia = YearMonth.of(ano, mes);

    Map<UUID, BigDecimal> previstos = new HashMap<>();
    orcamentoMensalRepository
        .buscarDoMes(usuarioId, ano, mes)
        .forEach(o -> previstos.put(o.getCategoria().getId(), o.getValorPrevisto()));

    Map<UUID, BigDecimal> realizados = new HashMap<>();
    orcamentoMensalRepository
        .buscarRealizados(usuarioId, competencia.atDay(1), competencia.atEndOfMonth())
        .forEach(v -> realizados.merge(v.getCategoriaId(), v.getValor(), BigDecimal::add));

    Bloco receitas = montarBloco(TipoCategoria.RECEITA, categorias, previstos, realizados);
    Bloco despesas = montarBloco(TipoCategoria.DESPESA, categorias, previstos, realizados);

    Valores saldo =
        valores(
            receitas.total().previsto().subtract(despesas.total().previsto()),
            receitas.total().realizado().subtract(despesas.total().realizado()));

    return new PrevistoRealizadoResponse(
        ano,
        mes,
        receitas,
        somarGruposDaFonte(receitas, FonteReceita.ATIVA),
        somarGruposDaFonte(receitas, FonteReceita.PASSIVA),
        despesas,
        saldo);
  }

  private Bloco montarBloco(
      TipoCategoria tipo,
      List<Categoria> categorias,
      Map<UUID, BigDecimal> previstos,
      Map<UUID, BigDecimal> realizados) {
    Map<UUID, List<Categoria>> filhosPorPai =
        categorias.stream()
            .filter(c -> c.getCategoriaPai() != null)
            .collect(Collectors.groupingBy(c -> c.getCategoriaPai().getId()));

    List<Grupo> grupos = new ArrayList<>();
    BigDecimal totalPrevisto = ZERO;
    BigDecimal totalRealizado = ZERO;

    for (Categoria grupo : categorias) {
      if (grupo.getTipo() != tipo || grupo.getCategoriaPai() != null) {
        continue;
      }

      BigDecimal previstoGrupo = Dinheiro.arredondar(previstos.get(grupo.getId()));
      BigDecimal realizadoGrupo = Dinheiro.arredondar(realizados.get(grupo.getId()));
      List<Item> subitens = new ArrayList<>();

      for (Categoria sub : filhosPorPai.getOrDefault(grupo.getId(), List.of())) {
        BigDecimal previsto = Dinheiro.arredondar(previstos.get(sub.getId()));
        BigDecimal realizado = Dinheiro.arredondar(realizados.get(sub.getId()));
        previstoGrupo = previstoGrupo.add(previsto);
        realizadoGrupo = realizadoGrupo.add(realizado);
        if (mostrar(sub, previsto, realizado)) {
          subitens.add(
              new Item(
                  sub.getId(),
                  sub.getNome(),
                  sub.getFonteReceita(),
                  Boolean.TRUE.equals(sub.getAtiva()),
                  valores(previsto, realizado)));
        }
      }

      if (!mostrar(grupo, previstoGrupo, realizadoGrupo)) {
        continue;
      }
      totalPrevisto = totalPrevisto.add(previstoGrupo);
      totalRealizado = totalRealizado.add(realizadoGrupo);
      grupos.add(
          new Grupo(
              grupo.getId(),
              grupo.getNome(),
              grupo.getFonteReceita(),
              Boolean.TRUE.equals(grupo.getAtiva()),
              valores(previstoGrupo, realizadoGrupo),
              subitens));
    }

    return new Bloco(valores(totalPrevisto, totalRealizado), grupos);
  }

  private static boolean mostrar(Categoria categoria, BigDecimal previsto, BigDecimal realizado) {
    return Boolean.TRUE.equals(categoria.getAtiva())
        || previsto.signum() != 0
        || realizado.signum() != 0;
  }

  private static Valores somarGruposDaFonte(Bloco receitas, FonteReceita fonte) {
    BigDecimal previsto = ZERO;
    BigDecimal realizado = ZERO;
    for (Grupo grupo : receitas.grupos()) {
      if (fonte == grupo.fonteReceita()) {
        previsto = previsto.add(grupo.valores().previsto());
        realizado = realizado.add(grupo.valores().realizado());
      }
    }
    return valores(previsto, realizado);
  }

  static Valores valores(BigDecimal previsto, BigDecimal realizado) {
    return new Valores(
        previsto, realizado, realizado.subtract(previsto), Fracao.de(realizado, previsto));
  }

  private boolean gravar(
      UUID usuarioId,
      Categoria categoria,
      YearMonth competencia,
      BigDecimal valor,
      Map<UUID, OrcamentoMensal> existentes,
      boolean substituirExistente) {
    OrcamentoMensal orcamento = existentes.get(categoria.getId());
    if (orcamento != null && !substituirExistente) {
      return false;
    }
    if (orcamento == null) {
      orcamento = novoOrcamento(usuarioId, categoria, competencia);
    }
    orcamento.setValorPrevisto(valor);
    orcamentoMensalRepository.save(orcamento);
    return true;
  }

  private Map<UUID, OrcamentoMensal> orcamentosDoMesPorCategoria(
      UUID usuarioId, YearMonth competencia) {
    return orcamentoMensalRepository
        .buscarDoMes(usuarioId, competencia.getYear(), competencia.getMonthValue())
        .stream()
        .collect(Collectors.toMap(o -> o.getCategoria().getId(), Function.identity()));
  }

  private OrcamentoMensal novoOrcamento(
      UUID usuarioId, Categoria categoria, YearMonth competencia) {
    return OrcamentoMensal.builder()
        .usuario(usuarioService.referencia(usuarioId))
        .categoria(categoria)
        .ano(competencia.getYear())
        .mes(competencia.getMonthValue())
        .tipo(categoria.getTipo())
        .build();
  }

  private static Categoria categoriaParaPrevisto(
      UUID categoriaId, Map<UUID, Categoria> porId, Set<UUID> comSubcategorias) {
    Categoria categoria = porId.get(categoriaId);
    if (categoria == null) {
      throw new ResourceNotFoundException("Categoria não encontrada.");
    }
    if (comSubcategorias.contains(categoriaId)) {
      throw new BusinessException(
          "Defina o previsto nas subcategorias de \""
              + categoria.getNome()
              + "\"; o grupo soma automaticamente.");
    }
    return categoria;
  }

  private static SortedSet<YearMonth> destinos(List<YearMonth> meses) {
    SortedSet<YearMonth> destinos = new TreeSet<>(meses);
    destinos.forEach(destino -> Competencia.validarAno(destino.getYear()));
    return destinos;
  }

  private static Map<UUID, Categoria> porId(List<Categoria> categorias) {
    return categorias.stream().collect(Collectors.toMap(Categoria::getId, Function.identity()));
  }

  private static Set<UUID> idsDosPais(List<Categoria> categorias) {
    return categorias.stream()
        .map(Categoria::getCategoriaPai)
        .filter(Objects::nonNull)
        .map(Categoria::getId)
        .collect(Collectors.toSet());
  }

  private static List<Categoria> ordenarComoArvore(List<Categoria> categorias) {
    Map<UUID, List<Categoria>> filhosPorPai =
        categorias.stream()
            .filter(c -> c.getCategoriaPai() != null)
            .collect(Collectors.groupingBy(c -> c.getCategoriaPai().getId()));
    List<Categoria> ordenadas = new ArrayList<>();
    categorias.stream()
        .filter(c -> c.getCategoriaPai() == null)
        .sorted(Comparator.comparing((Categoria c) -> c.getTipo() != TipoCategoria.RECEITA))
        .forEach(
            grupo -> {
              ordenadas.add(grupo);
              ordenadas.addAll(filhosPorPai.getOrDefault(grupo.getId(), List.of()));
            });
    return ordenadas;
  }

  private static Map<UUID, BigDecimal[]> porCategoriaEMes(List<ValorPorCategoriaEMes> valores) {
    Map<UUID, BigDecimal[]> resultado = new HashMap<>();
    for (ValorPorCategoriaEMes valor : valores) {
      BigDecimal[] meses =
          resultado.computeIfAbsent(valor.getCategoriaId(), k -> new BigDecimal[MESES_NO_ANO]);
      int i = valor.getMes() - 1;
      meses[i] = meses[i] == null ? valor.getValor() : meses[i].add(valor.getValor());
    }
    return resultado;
  }

  private static BigDecimal somar(BigDecimal[] meses) {
    BigDecimal total = ZERO;
    for (BigDecimal valor : meses) {
      if (valor != null) {
        total = total.add(valor);
      }
    }
    return total;
  }

  static BigDecimal mediaDosMesesComValor(BigDecimal[] meses) {
    int quantidade = 0;
    for (BigDecimal valor : meses) {
      if (valor != null) {
        quantidade++;
      }
    }
    if (quantidade == 0) {
      return ZERO;
    }
    return somar(meses)
        .divide(BigDecimal.valueOf(quantidade), Dinheiro.ESCALA, RoundingMode.HALF_UP);
  }
}
