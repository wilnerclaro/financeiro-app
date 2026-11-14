package br.com.financeiro.api.categoria.repository;

import br.com.financeiro.api.categoria.entity.Categoria;
import br.com.financeiro.api.categoria.enums.FonteReceita;
import br.com.financeiro.api.categoria.enums.TipoCategoria;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
  @EntityGraph(attributePaths = "categoriaPai")
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

  @Query(
      """
            SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END
            FROM Categoria c
            WHERE c.categoriaPai.id = :categoriaPaiId
            """)
  boolean existeSubcategoria(@Param("categoriaPaiId") UUID categoriaPaiId);

  @EntityGraph(attributePaths = "categoriaPai")
  @Query(
      """
            SELECT c
            FROM Categoria c
            WHERE c.usuario.id = :usuarioId
            ORDER BY c.nome
            """)
  List<Categoria> buscarTodasDoUsuario(@Param("usuarioId") UUID usuarioId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      """
            UPDATE Categoria c
            SET c.fonteReceita = :fonteReceita
            WHERE c.categoriaPai.id = :categoriaPaiId
            """)
  void atualizarFonteDasSubcategorias(
      @Param("categoriaPaiId") UUID categoriaPaiId,
      @Param("fonteReceita") FonteReceita fonteReceita);

  @Query(
      value =
          """
            SELECT EXISTS (SELECT 1 FROM lancamentos_financeiros WHERE categoria_id = :categoriaId)
                OR EXISTS (SELECT 1 FROM orcamentos_mensais WHERE categoria_id = :categoriaId)
            """,
      nativeQuery = true)
  boolean estaEmUso(@Param("categoriaId") UUID categoriaId);

  @Modifying
  @Query(
      value =
          """
            INSERT INTO categorias (usuario_id, nome, tipo, fonte_receita)
            SELECT :usuarioId, nome, tipo, fonte_receita
            FROM categorias_padrao
            WHERE categoria_pai_id IS NULL
              AND ativa
            """,
      nativeQuery = true)
  void copiarGruposPadrao(@Param("usuarioId") UUID usuarioId);

  @Modifying
  @Query(
      value =
          """
            INSERT INTO categorias (usuario_id, categoria_pai_id, nome, tipo, fonte_receita)
            SELECT :usuarioId, grupo.id, sub.nome, sub.tipo, sub.fonte_receita
            FROM categorias_padrao sub
            JOIN categorias_padrao pai ON pai.id = sub.categoria_pai_id
            JOIN categorias grupo
              ON grupo.usuario_id = :usuarioId
             AND grupo.categoria_pai_id IS NULL
             AND grupo.nome = pai.nome
             AND grupo.tipo = pai.tipo
            WHERE sub.ativa
              AND pai.ativa
            """,
      nativeQuery = true)
  void copiarSubcategoriasPadrao(@Param("usuarioId") UUID usuarioId);
}
