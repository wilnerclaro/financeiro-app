package br.com.financeiro.api.meta.repository;

import br.com.financeiro.api.meta.entity.Meta;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MetaRepository extends JpaRepository<Meta, UUID> {

  @Query(
      """
            SELECT m
            FROM Meta m
            WHERE m.usuario.id = :usuarioId
              AND (:incluirInativas = TRUE OR m.ativa = TRUE)
            ORDER BY m.criadoEm
            """)
  List<Meta> buscarDoUsuario(
      @Param("usuarioId") UUID usuarioId, @Param("incluirInativas") boolean incluirInativas);

  @Query("SELECT m FROM Meta m WHERE m.id = :id AND m.usuario.id = :usuarioId")
  Optional<Meta> buscarPorIdEUsuarioId(@Param("id") UUID id, @Param("usuarioId") UUID usuarioId);

  @Query(
      """
            SELECT COUNT(m) > 0
            FROM Meta m
            WHERE m.usuario.id = :usuarioId
              AND LOWER(m.nome) = LOWER(:nome)
              AND (:idIgnorado IS NULL OR m.id <> :idIgnorado)
            """)
  boolean existeComNome(
      @Param("usuarioId") UUID usuarioId,
      @Param("nome") String nome,
      @Param("idIgnorado") UUID idIgnorado);
}
