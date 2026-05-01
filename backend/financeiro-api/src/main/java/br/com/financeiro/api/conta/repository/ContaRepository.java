package br.com.financeiro.api.conta.repository;

import br.com.financeiro.api.conta.entity.Conta;
import br.com.financeiro.api.conta.enums.TipoConta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ContaRepository extends JpaRepository<Conta, UUID> {

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END
            FROM Conta c
            WHERE c.usuario.id = :usuarioId
              AND UPPER(c.nome) = UPPER(:nome) 
            """)
    boolean existeContaDuplicada(
            @Param("usuarioId") UUID usuarioId,
            @Param("nome") String nome
    );

    @Query("""
            SELECT c
            FROM Conta c
            WHERE c.usuario.id = :usuarioId
              AND (:tipo IS NULL OR c.tipo = :tipo)
              AND (:ativa IS NULL OR c.ativa = :ativa)
            """)
    Page<Conta> buscarPorFiltros(
            @Param("usuarioId") UUID usuarioId,
            @Param("tipo") TipoConta tipo,
            @Param("ativa") Boolean ativa,
            Pageable pageable
    );

    @Query("""
            SELECT c
            FROM Conta c
            WHERE c.id = :id
              AND c.usuario.id = :usuarioId
            """)
    Optional<Conta> buscarPorIdEUsuarioId(
            @Param("id") UUID id,
            @Param("usuarioId") UUID usuarioId
    );
}