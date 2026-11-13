package br.com.financeiro.api.categoria.service;

import br.com.financeiro.api.categoria.dto.AtualizarCategoriaRequest;
import br.com.financeiro.api.categoria.dto.CategoriaResponse;
import br.com.financeiro.api.categoria.dto.CriarCategoriaRequest;
import br.com.financeiro.api.categoria.entity.Categoria;
import br.com.financeiro.api.categoria.enums.FonteReceita;
import br.com.financeiro.api.categoria.enums.TipoCategoria;
import br.com.financeiro.api.categoria.mapper.CategoriaMapper;
import br.com.financeiro.api.categoria.repository.CategoriaRepository;
import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.exception.ResourceNotFoundException;
import br.com.financeiro.api.common.util.NormalizadorTexto;
import br.com.financeiro.api.usuario.entity.Usuario;
import br.com.financeiro.api.usuario.service.UsuarioService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoriaService {

  private final CategoriaRepository categoriaRepository;
  private final UsuarioService usuarioService;
  private final CategoriaMapper categoriaMapper;

  @Transactional
  public CategoriaResponse criar(UUID usuarioId, CriarCategoriaRequest request) {
    Usuario usuario = buscarUsuario(usuarioId);
    String nomeNormalizado = NormalizadorTexto.normalizarNome(request.nome());

    Categoria categoriaPai =
        buscarCategoriaPai(usuarioId, request.categoriaPaiId(), request.tipo());

    validarDuplicidade(usuarioId, nomeNormalizado, request.tipo(), request.categoriaPaiId(), null);

    Categoria categoria = categoriaMapper.paraEntity(request);
    categoria.setUsuario(usuario);
    categoria.setCategoriaPai(categoriaPai);
    categoria.setNome(nomeNormalizado);
    categoria.setAtiva(true);
    categoria.setFonteReceita(definirFonte(request.tipo(), categoriaPai, request.fonteReceita()));

    Categoria categoriaSalva = categoriaRepository.saveAndFlush(categoria);
    return categoriaMapper.paraResponse(categoriaSalva);
  }

  @Transactional(readOnly = true)
  public Page<CategoriaResponse> listar(
      UUID usuarioId, TipoCategoria tipo, Boolean ativa, Pageable pageable) {
    buscarUsuario(usuarioId);

    return categoriaRepository
        .buscarPorFiltros(usuarioId, tipo, ativa, pageable)
        .map(categoriaMapper::paraResponse);
  }

  @Transactional(readOnly = true)
  public CategoriaResponse buscarPorId(UUID id, UUID usuarioId) {
    Categoria categoria = buscarCategoriaDoUsuario(id, usuarioId);
    return categoriaMapper.paraResponse(categoria);
  }

  @Transactional
  public CategoriaResponse atualizar(UUID id, UUID usuarioId, AtualizarCategoriaRequest request) {
    Categoria categoria = buscarCategoriaDoUsuario(id, usuarioId);
    String nomeNormalizado = NormalizadorTexto.normalizarNome(request.nome());

    if (request.categoriaPaiId() != null && request.categoriaPaiId().equals(id)) {
      throw new BusinessException("A categoria não pode ser pai dela mesma.");
    }

    boolean possuiSubcategorias = categoriaRepository.existeSubcategoria(id);

    if (!categoria.getTipo().equals(request.tipo()) && possuiSubcategorias) {
      throw new BusinessException(
          "Não é possível alterar o tipo de uma categoria que possui subcategorias.");
    }

    if (request.categoriaPaiId() != null && possuiSubcategorias) {
      throw new BusinessException(
          "Uma categoria que possui subcategorias não pode virar subcategoria. "
              + "São permitidos apenas dois níveis.");
    }

    Categoria categoriaPai =
        buscarCategoriaPai(usuarioId, request.categoriaPaiId(), request.tipo());

    validarDuplicidade(usuarioId, nomeNormalizado, request.tipo(), request.categoriaPaiId(), id);

    categoria.setCategoriaPai(categoriaPai);
    categoria.setNome(nomeNormalizado);
    categoria.setTipo(request.tipo());
    categoria.setFonteReceita(definirFonte(request.tipo(), categoriaPai, request.fonteReceita()));
    categoriaRepository.flush();

    if (possuiSubcategorias) {
      categoriaRepository.atualizarFonteDasSubcategorias(id, categoria.getFonteReceita());
    }

    return categoriaMapper.paraResponse(categoria);
  }

  @Transactional
  public void inativar(UUID id, UUID usuarioId) {
    Categoria categoria = buscarCategoriaDoUsuario(id, usuarioId);

    if (categoriaRepository.existeSubcategoriaAtiva(id)) {
      throw new BusinessException(
          "Não é possível inativar uma categoria que possui subcategorias ativas.");
    }

    categoria.setAtiva(false);
  }

  @Transactional
  public CategoriaResponse ativar(UUID id, UUID usuarioId) {
    Categoria categoria = buscarCategoriaDoUsuario(id, usuarioId);
    Categoria categoriaPai = categoria.getCategoriaPai();

    if (categoriaPai != null && !Boolean.TRUE.equals(categoriaPai.getAtiva())) {
      throw new BusinessException(
          "Não é possível ativar uma subcategoria cuja categoria pai está inativa.");
    }

    categoria.setAtiva(true);
    categoriaRepository.flush();

    return categoriaMapper.paraResponse(categoria);
  }

  private FonteReceita definirFonte(
      TipoCategoria tipo, Categoria categoriaPai, FonteReceita fonteInformada) {
    if (!TipoCategoria.RECEITA.equals(tipo)) {
      return null;
    }

    if (categoriaPai != null) {
      return categoriaPai.getFonteReceita();
    }

    return fonteInformada == null ? FonteReceita.ATIVA : fonteInformada;
  }

  private Usuario buscarUsuario(UUID usuarioId) {
    return usuarioService.buscar(usuarioId);
  }

  @Transactional(readOnly = true)
  public Categoria buscarCategoriaAtiva(UUID categoriaId, UUID usuarioId) {
    Categoria categoria = buscarCategoriaDoUsuario(categoriaId, usuarioId);

    if (!Boolean.TRUE.equals(categoria.getAtiva())) {
      throw new BusinessException("A categoria informada está inativa.");
    }

    return categoria;
  }

  @Transactional(readOnly = true)
  public void validarCategoriaDoUsuario(UUID categoriaId, UUID usuarioId) {
    if (categoriaId != null) {
      buscarCategoriaDoUsuario(categoriaId, usuarioId);
    }
  }

  @Transactional(readOnly = true)
  public List<Categoria> listarTodasDoUsuario(UUID usuarioId) {
    return categoriaRepository.buscarTodasDoUsuario(usuarioId);
  }

  private Categoria buscarCategoriaDoUsuario(UUID categoriaId, UUID usuarioId) {
    return categoriaRepository
        .buscarPorIdEUsuarioId(categoriaId, usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));
  }

  private Categoria buscarCategoriaPai(UUID usuarioId, UUID categoriaPaiId, TipoCategoria tipo) {
    if (categoriaPaiId == null) {
      return null;
    }

    Categoria categoriaPai = buscarCategoriaDoUsuario(categoriaPaiId, usuarioId);

    if (!Boolean.TRUE.equals(categoriaPai.getAtiva())) {
      throw new BusinessException("A categoria pai informada está inativa.");
    }

    if (!categoriaPai.getTipo().equals(tipo)) {
      throw new BusinessException("A categoria pai deve possuir o mesmo tipo da categoria filha.");
    }

    if (categoriaPai.getCategoriaPai() != null) {
      throw new BusinessException(
          "A categoria pai informada já é uma subcategoria. O MVP permite apenas dois níveis.");
    }

    return categoriaPai;
  }

  private void validarDuplicidade(
      UUID usuarioId,
      String nome,
      TipoCategoria tipo,
      UUID categoriaPaiId,
      UUID categoriaIdIgnorada) {
    boolean duplicada =
        categoriaRepository.existeCategoriaDuplicada(
            usuarioId, nome, tipo, categoriaPaiId, categoriaIdIgnorada);

    if (duplicada) {
      throw new BusinessException("Já existe uma categoria com esse nome, tipo e categoria pai.");
    }
  }
}
