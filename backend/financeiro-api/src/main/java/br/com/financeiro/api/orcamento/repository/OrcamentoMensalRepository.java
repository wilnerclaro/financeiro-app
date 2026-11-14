package br.com.financeiro.api.orcamento.repository;

import br.com.financeiro.api.orcamento.entity.OrcamentoMensal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrcamentoMensalRepository extends JpaRepository<OrcamentoMensal, UUID> {

  interface ValorPorCategoriaEMes {
    UUID getCategoriaId();

    Integer getMes();

    BigDecimal getValor();
  }

  @EntityGraph(attributePaths = "categoria")
  @Query(
      """
            SELECT o
            FROM OrcamentoMensal o
            WHERE o.usuario.id = :usuarioId
              AND o.ano = :ano
              AND o.mes = :mes
            """)
  List<OrcamentoMensal> buscarDoMes(
      @Param("usuarioId") UUID usuarioId, @Param("ano") Integer ano, @Param("mes") Integer mes);

  @Query(
      """
            SELECT o.categoria.id AS categoriaId, o.mes AS mes, o.valorPrevisto AS valor
            FROM OrcamentoMensal o
            WHERE o.usuario.id = :usuarioId
              AND o.ano = :ano
            """)
  List<ValorPorCategoriaEMes> buscarPrevistosDoAno(
      @Param("usuarioId") UUID usuarioId, @Param("ano") Integer ano);

  @Query(
      value =
          """
            SELECT categoria_id AS categoriaId,
                   CAST(EXTRACT(MONTH FROM data_competencia) AS INTEGER) AS mes,
                   SUM(valor) AS valor
            FROM lancamentos_financeiros
            WHERE usuario_id = :usuarioId
              AND status = 'PAGO'
              AND data_competencia BETWEEN :inicio AND :fim
            GROUP BY categoria_id, EXTRACT(MONTH FROM data_competencia)
            """,
      nativeQuery = true)
  List<ValorPorCategoriaEMes> buscarRealizados(
      @Param("usuarioId") UUID usuarioId,
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim);
}
