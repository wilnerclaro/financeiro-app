package br.com.financeiro.api.categoria.repository;

import br.com.financeiro.api.categoria.entity.Categoria;
import br.com.financeiro.api.categoria.enums.TipoCategoria;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {

  @Query(
      """
            SELECT c
            FROM Categoria c
            WHERE c.usuario.id = :usuarioId
              AND (:tipo IS NULL OR c.tipo = :tipo)
              AND (:ativa IS NULL OR c.ativa = :ativa)
            """)
  Page<Categoria> buscarPorFiltros(
      @Param("usuarioId") UUID usuarioId,
      @Param("tipo") TipoCategoria tipo,
      @Param("ativa") Boolean ativa,
      Pageable pageable);

  @Query(
      """
            SELECT c
            FROM Categoria c
            WHERE c.id = :id
              AND c.usuario.id = :usuarioId
            """)
  Optional<Categoria> buscarPorIdEUsuarioId(
      @Param("id") UUID id, @Param("usuarioId") UUID usuarioId);

  @Query(
      """
            SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END
            FROM Categoria c
            WHERE c.usuario.id = :usuarioId
              AND UPPER(c.nome) = UPPER(:nome)
              AND c.tipo = :tipo
              AND (
                    (:categoriaPaiId IS NULL AND c.categoriaPai IS NULL)
                    OR c.categoriaPai.id = :categoriaPaiId
                  )
              AND (:categoriaIdIgnorada IS NULL OR c.id <> :categoriaIdIgnorada)
            """)
  boolean existeCategoriaDuplicada(
      @Param("usuarioId") UUID usuarioId,
      @Param("nome") String nome,
      @Param("tipo") TipoCategoria tipo,
      @Param("categoriaPaiId") UUID categoriaPaiId,
      @Param("categoriaIdIgnorada") UUID categoriaIdIgnorada);

  @Query(
      """
            SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END
            FROM Categoria c
            WHERE c.categoriaPai.id = :categoriaPaiId
              AND c.ativa = TRUE
            """)
  boolean existeSubcategoriaAtiva(@Param("categoriaPaiId") UUID categoriaPaiId);
}
