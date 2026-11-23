package br.com.financeiro.api.meta.repository;

import br.com.financeiro.api.meta.entity.MovimentoMeta;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimentoMetaRepository extends JpaRepository<MovimentoMeta, UUID> {

  @Query(
      """
            SELECT mm
            FROM MovimentoMeta mm
            WHERE mm.meta.id IN :metaIds
            ORDER BY mm.ano, mm.mes
            """)
  List<MovimentoMeta> buscarDasMetas(@Param("metaIds") Collection<UUID> metaIds);

  @Query(
      """
            SELECT mm
            FROM MovimentoMeta mm
            WHERE mm.meta.id = :metaId
              AND mm.ano = :ano
              AND mm.mes = :mes
            """)
  Optional<MovimentoMeta> buscarDoMes(
      @Param("metaId") UUID metaId, @Param("ano") Integer ano, @Param("mes") Integer mes);
}
