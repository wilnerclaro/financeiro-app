package br.com.financeiro.api.orcamento.service;

import br.com.financeiro.api.categoria.entity.Categoria;
import br.com.financeiro.api.categoria.enums.FonteReceita;
import br.com.financeiro.api.categoria.enums.TipoCategoria;
import br.com.financeiro.api.categoria.service.CategoriaService;
import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.exception.ResourceNotFoundException;
import br.com.financeiro.api.common.util.Fracao;
import br.com.financeiro.api.orcamento.dto.DefinirOrcamentoRequest;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse.Bloco;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse.Grupo;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse.Item;
import br.com.financeiro.api.orcamento.dto.PrevistoRealizadoResponse.Valores;
import br.com.financeiro.api.orcamento.dto.ResumoAnualOrcamentoResponse;
import br.com.financeiro.api.orcamento.entity.OrcamentoMensal;
import br.com.financeiro.api.orcamento.repository.OrcamentoMensalRepository;
import br.com.financeiro.api.orcamento.repository.OrcamentoMensalRepository.ValorPorCategoriaEMes;
import br.com.financeiro.api.usuario.service.UsuarioService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrcamentoService {

  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

  private final OrcamentoMensalRepository orcamentoMensalRepository;
  private final CategoriaService categoriaService;
  private final UsuarioService usuarioService;

  @Transactional
  public PrevistoRealizadoResponse definir(
      UUID usuarioId, int ano, int mes, DefinirOrcamentoRequest request) {
    validarCompetencia(ano, mes);

    List<Categoria> categorias = categoriaService.listarTodasDoUsuario(usuarioId);
    Map<UUID, Categoria> porId =
        categorias.stream().collect(Collectors.toMap(Categoria::getId, Function.identity()));
    Set<UUID> comSubcategorias = idsDosPais(categorias);

    Set<UUID> informadas = new HashSet<>();
    for (DefinirOrcamentoRequest.Item item : request.itens()) {
      if (!informadas.add(item.categoriaId())) {
        throw new BusinessException("A mesma categoria foi informada mais de uma vez.");
      }
      Categoria categoria = porId.get(item.categoriaId());
      if (categoria == null) {
        throw new ResourceNotFoundException("Categoria não encontrada.");
      }
      if (comSubcategorias.contains(categoria.getId())) {
        throw new BusinessException(
            "Defina o previsto nas subcategorias de \""
                + categoria.getNome()
                + "\"; o grupo soma automaticamente.");
      }
    }

    Map<UUID, OrcamentoMensal> existentes = orcamentosDoMesPorCategoria(usuarioId, ano, mes);
    for (DefinirOrcamentoRequest.Item item : request.itens()) {
      OrcamentoMensal orcamento = existentes.get(item.categoriaId());
      if (orcamento == null) {
        orcamento = novoOrcamento(usuarioId, porId.get(item.categoriaId()), ano, mes);
      }
      orcamento.setValorPrevisto(item.valorPrevisto().setScale(2, RoundingMode.HALF_UP));
      orcamentoMensalRepository.save(orcamento);
    }
    orcamentoMensalRepository.flush();

    return montarRelatorio(usuarioId, ano, mes, categorias);
  }

  @Transactional
  public PrevistoRealizadoResponse copiarMesAnterior(UUID usuarioId, int ano, int mes) {
    validarCompetencia(ano, mes);
    YearMonth anterior = YearMonth.of(ano, mes).minusMonths(1);

    List<OrcamentoMensal> doMesAnterior =
        orcamentoMensalRepository.buscarDoMes(
            usuarioId, anterior.getYear(), anterior.getMonthValue());
    if (doMesAnterior.isEmpty()) {
      throw new BusinessException("O mês anterior não tem previsto para copiar.");
    }

    Map<UUID, OrcamentoMensal> existentes = orcamentosDoMesPorCategoria(usuarioId, ano, mes);
    for (OrcamentoMensal origem : doMesAnterior) {
      if (!existentes.containsKey(origem.getCategoria().getId())) {
        OrcamentoMensal copia = novoOrcamento(usuarioId, origem.getCategoria(), ano, mes);
        copia.setValorPrevisto(origem.getValorPrevisto());
        orcamentoMensalRepository.save(copia);
      }
    }
    orcamentoMensalRepository.flush();

    return montarRelatorio(usuarioId, ano, mes, categoriaService.listarTodasDoUsuario(usuarioId));
  }

  @Transactional(readOnly = true)
  public PrevistoRealizadoResponse previstoRealizado(UUID usuarioId, int ano, int mes) {
    validarCompetencia(ano, mes);
    return montarRelatorio(usuarioId, ano, mes, categoriaService.listarTodasDoUsuario(usuarioId));
  }

  @Transactional(readOnly = true)
  public ResumoAnualOrcamentoResponse resumoAnual(UUID usuarioId, int ano) {
    validarCompetencia(ano, 1);

    List<Categoria> categorias = categoriaService.listarTodasDoUsuario(usuarioId);
    Map<UUID, BigDecimal[]> previstos =
        porCategoriaEMes(orcamentoMensalRepository.buscarPrevistosDoAno(usuarioId, ano));
    Map<UUID, BigDecimal[]> realizados =
        porCategoriaEMes(
            orcamentoMensalRepository.buscarRealizados(
                usuarioId, YearMonth.of(ano, 1).atDay(1), YearMonth.of(ano, 12).atEndOfMonth()));

    List<ResumoAnualOrcamentoResponse.Linha> linhas = new ArrayList<>();
    for (Categoria categoria : ordenarComoArvore(categorias)) {
      BigDecimal[] previsto = previstos.getOrDefault(categoria.getId(), new BigDecimal[12]);
      BigDecimal[] realizado = realizados.getOrDefault(categoria.getId(), new BigDecimal[12]);
      boolean temValor =
          previstos.containsKey(categoria.getId()) || realizados.containsKey(categoria.getId());
      if (!Boolean.TRUE.equals(categoria.getAtiva()) && !temValor) {
        continue;
      }

      List<ResumoAnualOrcamentoResponse.Mes> meses = new ArrayList<>();
      for (int i = 0; i < 12; i++) {
        meses.add(
            new ResumoAnualOrcamentoResponse.Mes(
                i + 1, valorOuZero(previsto[i]), valorOuZero(realizado[i])));
      }
      linhas.add(
          new ResumoAnualOrcamentoResponse.Linha(
              categoria.getId(),
              categoria.getNome(),
              categoria.getCategoriaPai() == null ? null : categoria.getCategoriaPai().getId(),
              categoria.getTipo().name(),
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

      BigDecimal previstoGrupo = valorOuZero(previstos.get(grupo.getId()));
      BigDecimal realizadoGrupo = valorOuZero(realizados.get(grupo.getId()));
      List<Item> subitens = new ArrayList<>();

      for (Categoria sub : filhosPorPai.getOrDefault(grupo.getId(), List.of())) {
        BigDecimal previsto = valorOuZero(previstos.get(sub.getId()));
        BigDecimal realizado = valorOuZero(realizados.get(sub.getId()));
        previstoGrupo = previstoGrupo.add(previsto);
        realizadoGrupo = realizadoGrupo.add(realizado);
        if (mostrar(sub, previsto, realizado)) {
          subitens.add(
              new Item(
                  sub.getId(),
                  sub.getNome(),
                  sub.getFonteReceita() == null ? null : sub.getFonteReceita().name(),
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
              grupo.getFonteReceita() == null ? null : grupo.getFonteReceita().name(),
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
      if (fonte.name().equals(grupo.fonteReceita())) {
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

  private Map<UUID, OrcamentoMensal> orcamentosDoMesPorCategoria(UUID usuarioId, int ano, int mes) {
    return orcamentoMensalRepository.buscarDoMes(usuarioId, ano, mes).stream()
        .collect(Collectors.toMap(o -> o.getCategoria().getId(), Function.identity()));
  }

  private OrcamentoMensal novoOrcamento(UUID usuarioId, Categoria categoria, int ano, int mes) {
    return OrcamentoMensal.builder()
        .usuario(usuarioService.referencia(usuarioId))
        .categoria(categoria)
        .ano(ano)
        .mes(mes)
        .tipo(categoria.getTipo())
        .build();
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
          resultado.computeIfAbsent(valor.getCategoriaId(), k -> new BigDecimal[12]);
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
    return somar(meses).divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
  }

  private static BigDecimal valorOuZero(BigDecimal valor) {
    return valor == null ? ZERO : valor.setScale(2, RoundingMode.HALF_UP);
  }

  private static void validarCompetencia(int ano, int mes) {
    if (ano < 2000 || ano > 2100) {
      throw new BusinessException("O ano deve estar entre 2000 e 2100.");
    }
    if (mes < 1 || mes > 12) {
      throw new BusinessException("O mês deve estar entre 1 e 12.");
    }
  }
}
