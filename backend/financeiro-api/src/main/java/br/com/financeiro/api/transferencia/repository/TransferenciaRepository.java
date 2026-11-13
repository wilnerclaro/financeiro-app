package br.com.financeiro.api.transferencia.repository;

import br.com.financeiro.api.transferencia.entity.Transferencia;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferenciaRepository extends JpaRepository<Transferencia, UUID> {

  @EntityGraph(attributePaths = {"contaOrigem", "contaDestino"})
  @Query(
      """
            SELECT t
            FROM Transferencia t
            WHERE t.usuario.id = :usuarioId
              AND (:contaId IS NULL OR t.contaOrigem.id = :contaId OR t.contaDestino.id = :contaId)
              AND (CAST(:dataInicio AS LocalDate) IS NULL OR t.dataTransferencia >= :dataInicio)
              AND (CAST(:dataFim AS LocalDate) IS NULL OR t.dataTransferencia <= :dataFim)
            """)
  Page<Transferencia> buscarPorFiltros(
      @Param("usuarioId") UUID usuarioId,
      @Param("contaId") UUID contaId,
      @Param("dataInicio") LocalDate dataInicio,
      @Param("dataFim") LocalDate dataFim,
      Pageable pageable);

  @EntityGraph(attributePaths = {"contaOrigem", "contaDestino"})
  @Query(
      """
            SELECT t
            FROM Transferencia t
            WHERE t.id = :id
              AND t.usuario.id = :usuarioId
            """)
  Optional<Transferencia> buscarPorIdEUsuarioId(
      @Param("id") UUID id, @Param("usuarioId") UUID usuarioId);
}
