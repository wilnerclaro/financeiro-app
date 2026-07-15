package br.com.financeiro.api.categoria.service;

import br.com.financeiro.api.categoria.dto.AtualizarCategoriaRequest;
import br.com.financeiro.api.categoria.dto.CategoriaResponse;
import br.com.financeiro.api.categoria.dto.CriarCategoriaRequest;
import br.com.financeiro.api.categoria.entity.Categoria;
import br.com.financeiro.api.categoria.enums.TipoCategoria;
import br.com.financeiro.api.categoria.mapper.CategoriaMapper;
import br.com.financeiro.api.categoria.repository.CategoriaRepository;
import br.com.financeiro.api.common.exception.BusinessException;
import br.com.financeiro.api.common.exception.ResourceNotFoundException;
import br.com.financeiro.api.common.util.NormalizadorTexto;
import br.com.financeiro.api.usuario.entity.Usuario;
import br.com.financeiro.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaMapper categoriaMapper;

    @Transactional
    public CategoriaResponse criar(CriarCategoriaRequest request) {
        Usuario usuario = buscarUsuario(request.usuarioId());
        String nomeNormalizado = NormalizadorTexto.normalizarNome(request.nome());

        Categoria categoriaPai = buscarCategoriaPai(
                request.usuarioId(),
                request.categoriaPaiId(),
                request.tipo()
        );

        validarDuplicidade(
                request.usuarioId(),
                nomeNormalizado,
                request.tipo(),
                request.categoriaPaiId(),
                null
        );

        Categoria categoria = categoriaMapper.paraEntity(request);
        categoria.setUsuario(usuario);
        categoria.setCategoriaPai(categoriaPai);
        categoria.setNome(nomeNormalizado.toUpperCase());
        categoria.setAtiva(true);

        Categoria categoriaSalva = categoriaRepository.save(categoria);
        return categoriaMapper.paraResponse(categoriaSalva);
    }

    @Transactional(readOnly = true)
    public Page<CategoriaResponse> listar(
            UUID usuarioId,
            TipoCategoria tipo,
            Boolean ativa,
            Pageable pageable
    ) {
        buscarUsuario(usuarioId);

        return categoriaRepository.buscarPorFiltros(usuarioId, tipo, ativa, pageable)
                .map(categoriaMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(UUID id, UUID usuarioId) {
        Categoria categoria = buscarCategoriaDoUsuario(id, usuarioId);
        return categoriaMapper.paraResponse(categoria);
    }

    @Transactional
    public CategoriaResponse atualizar(UUID id, AtualizarCategoriaRequest request) {
        Categoria categoria = buscarCategoriaDoUsuario(id, request.usuarioId());
        String nomeNormalizado = NormalizadorTexto.normalizarNome(request.nome());

        if (request.categoriaPaiId() != null && request.categoriaPaiId().equals(id)) {
            throw new BusinessException("A categoria nao pode ser pai dela mesma.");
        }

        if (!categoria.getTipo().equals(request.tipo())
                && categoriaRepository.existeSubcategoriaAtiva(id)) {
            throw new BusinessException(
                    "Nao e possivel alterar o tipo de uma categoria que possui subcategorias ativas."
            );
        }

        Categoria categoriaPai = buscarCategoriaPai(
                request.usuarioId(),
                request.categoriaPaiId(),
                request.tipo()
        );

        validarDuplicidade(
                request.usuarioId(),
                nomeNormalizado,
                request.tipo(),
                request.categoriaPaiId(),
                id
        );

        categoria.setCategoriaPai(categoriaPai);
        categoria.setNome(nomeNormalizado.toUpperCase());
        categoria.setTipo(request.tipo());
        categoria.setAtiva(request.ativa());

        return categoriaMapper.paraResponse(categoria);
    }

    @Transactional
    public void inativar(UUID id, UUID usuarioId) {
        Categoria categoria = buscarCategoriaDoUsuario(id, usuarioId);

        if (categoriaRepository.existeSubcategoriaAtiva(id)) {
            throw new BusinessException(
                    "Nao e possivel inativar uma categoria que possui subcategorias ativas."
            );
        }

        categoria.setAtiva(false);
    }

    @Transactional
    public CategoriaResponse ativar(UUID id, UUID usuarioId) {
        Categoria categoria = buscarCategoriaDoUsuario(id, usuarioId);
        categoria.setAtiva(true);

        return categoriaMapper.paraResponse(categoria);
    }

    private Usuario buscarUsuario(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));
    }

    private Categoria buscarCategoriaDoUsuario(UUID categoriaId, UUID usuarioId) {
        return categoriaRepository.buscarPorIdEUsuarioId(categoriaId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria nao encontrada."));
    }

    private Categoria buscarCategoriaPai(
            UUID usuarioId,
            UUID categoriaPaiId,
            TipoCategoria tipo
    ) {
        if (categoriaPaiId == null) {
            return null;
        }

        Categoria categoriaPai = buscarCategoriaDoUsuario(categoriaPaiId, usuarioId);

        if (!categoriaPai.getAtiva()) {
            throw new BusinessException("A categoria pai informada esta inativa.");
        }

        if (!categoriaPai.getTipo().equals(tipo)) {
            throw new BusinessException(
                    "A categoria pai deve possuir o mesmo tipo da categoria filha."
            );
        }

        if (categoriaPai.getCategoriaPai() != null) {
            throw new BusinessException(
                    "A categoria pai informada ja e uma subcategoria. O MVP permite apenas dois niveis."
            );
        }

        return categoriaPai;
    }

    private void validarDuplicidade(
            UUID usuarioId,
            String nome,
            TipoCategoria tipo,
            UUID categoriaPaiId,
            UUID categoriaIdIgnorada
    ) {
        boolean duplicada = categoriaRepository.existeCategoriaDuplicada(
                usuarioId,
                nome,
                tipo,
                categoriaPaiId,
                categoriaIdIgnorada
        );

        if (duplicada) {
            throw new BusinessException("Ja existe uma categoria com esse nome, tipo e categoria pai.");
        }
    }
}
